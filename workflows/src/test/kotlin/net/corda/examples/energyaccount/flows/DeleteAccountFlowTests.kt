package net.corda.examples.energyaccount.flows

import junit.framework.TestCase.assertEquals
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.node.services.Vault
import net.corda.examples.energyaccount.contracts.AccountState
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.text.StringContainsInOrder
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class DeleteAccountFlowTests : AccountFlowTestBase() {

    lateinit var existingAccountLinearId: UniqueIdentifier

    @Before
    fun initialiseExistingAccount() {
        val stx = createAccount(supplierA, defaultCustomerDetails)

        network.waitQuiescent()

        existingAccountLinearId = (stx.tx.outputs.single().data as AccountState).linearId
    }

    @After
    fun clearAccountId() {
        existingAccountLinearId = UniqueIdentifier()
    }

    @Test
    fun `Flow rejects account deletion for invalid account id`() {

        val ex = assertFailsWith<FlowException> {
            deleteAccount(supplierA, UniqueIdentifier("Invalid id"))
        }

        assertThat(ex.message, StringContainsInOrder(listOf("Account with id ", " not found.")))
    }

    @Test
    fun `Flow rejects account deletion for a previously deleted account`() {

        // Setup: Delete the account
        val stx = deleteAccount(supplierA, existingAccountLinearId)

        network.waitQuiescent()

        // Validate the transaction has a single input state, and no output states
        assertEquals(1, stx.tx.inputs.size)
        assert(stx.tx.outputs.isEmpty())

        // Attempt to delete again
        val ex = assertFailsWith<FlowException> {
            deleteAccount(supplierA, existingAccountLinearId)
        }

        assertThat(ex.message, StringContainsInOrder(listOf("Account with id ", " not found.")))
    }

    @Test
    fun `Flow rejects account deletion for an account owned by another supplier`() {

        val ex = assertFailsWith<FlowException> {
            deleteAccount(supplierB, existingAccountLinearId)
        }

        assertThat(ex.message, StringContainsInOrder(listOf("Account with id ", " not found.")))
    }

    @Test
    fun `Flow successfully deletes a valid existing account`() {
        val stx = deleteAccount(supplierA, existingAccountLinearId)

        network.waitQuiescent()

        // Validate both supplier and regulator signed the transaction
        stx.verifyRequiredSignatures()

        // Validate the transaction has a single input state, and no output states
        assertEquals(1, stx.tx.inputs.size)
        assert(stx.tx.outputs.isEmpty())

        // Validate the transaction is recorded in both parties transaction storage and the account
        // is consumed
        listOf(supplierA, regulator).forEach {
            verifyNodeRecordedTxn(it, stx)
            verifyNodeHasStates(it, 0, Vault.StateStatus.UNCONSUMED)
            verifyNodeHasStates(it, 1, Vault.StateStatus.CONSUMED)
        }
    }
}
