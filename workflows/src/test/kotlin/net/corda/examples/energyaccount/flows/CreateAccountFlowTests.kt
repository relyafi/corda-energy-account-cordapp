package net.corda.examples.energyaccount.flows

import net.corda.core.contracts.hash
import net.corda.core.flows.FlowException
import net.corda.core.node.services.queryBy
import net.corda.examples.energyaccount.contracts.AccountState
import net.corda.testing.core.singleIdentity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertFailsWith

class CreateAccountFlowTests : AccountFlowTestBase() {

    @Test
    fun `Flow rejects account creation that fails contract validation`() {

        val ex = assertFailsWith<FlowException> {
            createAccount(supplierA,
                          defaultCustomerDetails.copy(firstName = ""))
        }

        assertThat(ex.message, containsString("Failed requirement: Name is populated"))
    }

    @Test
    fun `Flow successfully creates a valid new account`() {
        val stx = createAccount(supplierA, defaultCustomerDetails)

        network.waitQuiescent()

        // Validate both supplier and regulator signed the transaction
        stx.verifyRequiredSignatures()

        // Validate the transaction has no input states, and an output state corresponding
        // to our account attributes
        assertEquals(0, stx.tx.inputs.size)
        assertEquals(1, stx.tx.outputs.size)

        val outState = stx.tx.outputs[0].data as AccountState

        assertEquals(supplierA.info.singleIdentity(), outState.supplier)
        assertEquals("John", outState.customerDetails.firstName)
        assertEquals("Smith", outState.customerDetails.lastName)

        // Validate the transaction is recorded in both parties transaction storage
        assertEquals(stx, supplierA.services.validatedTransactions.getTransaction(stx.id))
        assertEquals(stx, regulator.services.validatedTransactions.getTransaction(stx.id))

        // Validate the account exists in both parties vaults
        regulator.transaction {
            val accounts = regulator.services.vaultService.queryBy<AccountState>().states
            assertEquals(1, accounts.size)
            assertEquals(outState.hash(), accounts.single().state.data.hash())
        }

        supplierA.transaction {
            val accounts = supplierA.services.vaultService.queryBy<AccountState>().states
            assertEquals(1,accounts.size)
            assertEquals(outState.hash(), accounts.single().state.data.hash())
        }
    }
}
