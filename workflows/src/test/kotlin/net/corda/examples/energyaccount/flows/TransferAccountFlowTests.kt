package net.corda.examples.energyaccount.flows

import junit.framework.TestCase
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.hash
import net.corda.core.flows.FlowException
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.examples.energyaccount.contracts.AccountState
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.StartedMockNode
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.text.StringContainsInOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class TransferAccountFlowTests : AccountFlowTestBase() {

    lateinit var existingAccountLinearId: UniqueIdentifier

    @Before
    fun initialiseExistingAccount() {
        val stx = createAcount(supplierA, "Alice", "Anderson")

        network.waitQuiescent()

        existingAccountLinearId = (stx.tx.outputs.single().data as AccountState).linearId
    }

    @After
    fun clearAccountId() {
        existingAccountLinearId = UniqueIdentifier()
    }

    @Test
    fun `Flow rejects account transfer for invalid account id`() {

        val ex = assertFailsWith<FlowException> {
            transferAccount(
                    supplierA,
                    UniqueIdentifier("Invalid id"),
                    supplierB.info.singleIdentity())
        }

        assertThat(ex.message, StringContainsInOrder(listOf("Account with id ", " not found.")))
    }

    @Test
    fun `Flow rejects account transfer to same provider as current`() {

        val ex = assertFailsWith<FlowException> {
            transferAccount(
                    supplierA,
                    existingAccountLinearId,
                    supplierA.info.singleIdentity())
        }

        assertThat(ex.message, containsString(
                "Failed requirement: The old and new supplier must differ"))
    }

    @Test
    fun `Flow accepts account transfer to different provider`() {

        val stx = transferAccount(
                supplierA,
                existingAccountLinearId,
                supplierB.info.singleIdentity())

        network.waitQuiescent()

        // Validate suppliers and regulator signed the transaction
        stx.verifyRequiredSignatures()

        // Validate the transaction has a single input and output state
        assertEquals(1, stx.tx.inputs.size)
        assertEquals(1, stx.tx.outputs.size)

        // Validate the new state refers to the new supplier
        val outState = stx.tx.outputs[0].data as AccountState
        assertEquals(supplierB.info.singleIdentity(), outState.supplier)
        
        // Validate the transaction is recorded in all parties transaction storage
        listOf(supplierA, supplierB, regulator).forEach {
            verifyNodeRecordedTxn(it, stx)
        }

        // Validate the old supplier (A) only sees the old "consumed" state, the new supplier (B)
        // only sees the new "unconsumed" state, and the regulator sees both
        verifyNodeHasStates(supplierA, 0, Vault.StateStatus.UNCONSUMED)
        verifyNodeHasStates(supplierA, 1, Vault.StateStatus.CONSUMED)

        verifyNodeHasStates(supplierB, 1, Vault.StateStatus.UNCONSUMED)
        verifyNodeHasStates(supplierB, 0, Vault.StateStatus.CONSUMED)

        verifyNodeHasStates(regulator, 1, Vault.StateStatus.UNCONSUMED)
        verifyNodeHasStates(regulator, 1, Vault.StateStatus.CONSUMED)
    }

    @Test
    fun `Flow accepts account transfer to a provider that has previously held account`() {

        // Setup by transferring A->B, then rest of the test handles B->A
        transferAccount(
                supplierA,
                existingAccountLinearId,
                supplierB.info.singleIdentity())

        network.waitQuiescent()

        val stx = transferAccount(
                supplierB,
                existingAccountLinearId,
                supplierA.info.singleIdentity())

        network.waitQuiescent()

        // Validate suppliers and regulator signed the transaction
        stx.verifyRequiredSignatures()

        // Validate the transaction has a single input and output state
        assertEquals(1, stx.tx.inputs.size)
        assertEquals(1, stx.tx.outputs.size)

        // Validate the new state refers to the new supplier
        val outState = stx.tx.outputs[0].data as AccountState
        assertEquals(supplierA.info.singleIdentity(), outState.supplier)

        // Validate the transaction is recorded in all parties transaction storage
        listOf(supplierA, supplierB, regulator).forEach {
            verifyNodeRecordedTxn(it, stx)
        }

        // Validate the old supplier (B) only sees the old "consumed" state, the new supplier (A)
        // sees it's original "consumed" state and subsequent "unconsomed" state, and the regulator
        // sees all 3
        verifyNodeHasStates(supplierA, 1, Vault.StateStatus.UNCONSUMED)
        verifyNodeHasStates(supplierA, 1, Vault.StateStatus.CONSUMED)

        verifyNodeHasStates(supplierB, 0, Vault.StateStatus.UNCONSUMED)
        verifyNodeHasStates(supplierB, 1, Vault.StateStatus.CONSUMED)

        verifyNodeHasStates(regulator, 1, Vault.StateStatus.UNCONSUMED)
        verifyNodeHasStates(regulator, 2, Vault.StateStatus.CONSUMED)
    }
}
