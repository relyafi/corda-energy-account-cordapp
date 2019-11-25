package net.corda.examples.energyaccount.flows

import junit.framework.TestCase.assertEquals
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.node.services.queryBy
import net.corda.examples.energyaccount.contracts.AccountState
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.text.StringContainsInOrder
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class DestroyAccountFlowTests : AccountFlowTestBase() {

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
    fun `Flow rejects account deletion for invalid account id`() {

        val ex = assertFailsWith<FlowException> {
            destroyAccount(supplierA, UniqueIdentifier("Invalid id"))
        }

        assertThat(ex.message, StringContainsInOrder(listOf("Account with id ", " not found.")))
    }

    @Test
    fun `Flow rejects account deletion for a previously deleted account`() {

        // Setup: Delete the account
        val stx = destroyAccount(supplierA, existingAccountLinearId)

        network.waitQuiescent()

        // Validate the transaction has a single input state, and no output states
        assertEquals(1, stx.tx.inputs.size)
        assert(stx.tx.outputs.isEmpty())

        // Attempt to delete again
        val ex = assertFailsWith<FlowException> {
            destroyAccount(supplierA, existingAccountLinearId)
        }

        assertThat(ex.message, StringContainsInOrder(listOf("Account with id ", " not found.")))
    }

    @Test
    fun `Flow rejects account deletion for an account owned by another supplier`() {

        val ex = assertFailsWith<FlowException> {
            destroyAccount(supplierB, existingAccountLinearId)
        }

        assertThat(ex.message, StringContainsInOrder(listOf("Account with id ", " not found.")))
    }

    @Test
    fun `Flow successfully deletes a valid existing account`() {
        val stx = destroyAccount(supplierA, existingAccountLinearId)

        network.waitQuiescent()

        // Validate both supplier and regulator signed the transaction
        stx.verifyRequiredSignatures()

        // Validate the transaction has a single input state, and no output states
        assertEquals(1, stx.tx.inputs.size)
        assert(stx.tx.outputs.isEmpty())

        // Validate the transaction is recorded in both parties transaction storage
        assertEquals(stx, supplierA.services.validatedTransactions.getTransaction(stx.id))
        assertEquals(stx, regulator.services.validatedTransactions.getTransaction(stx.id))

        // Validate the account no longer exists in both parties vaults
        regulator.transaction {
            val accounts = regulator.services.vaultService.queryBy<AccountState>().states
            assert(accounts.isEmpty())
        }

        supplierA.transaction {
            val accounts = supplierA.services.vaultService.queryBy<AccountState>().states
            assert(accounts.isEmpty())
        }
    }
}
