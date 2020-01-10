package net.corda.examples.energyaccount.contracts

import net.corda.testing.node.transaction
import org.junit.Test

class AccountContractDeleteTests : AccountContractTestBase() {

    @Test
    fun `Output state provided`() {
        ledgerServices.transaction() {
            input(AccountContract.ID, defaultState)
            command(defaultSigners, AccountContract.Commands.Delete())
            output(AccountContract.ID, defaultState)
            `fails with` ("No outputs should be created")
        }
    }

    @Test
    fun `Wrong number of input states provided`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.Delete())
            `fails with` ("A transaction must contain at least one input or output state")
            input(AccountContract.ID, defaultState)
            input(AccountContract.ID,
                  defaultState.copy(
                          customerDetails = defaultState.customerDetails.copy(
                                  firstName = "Alice",
                                  lastName = "Anderson")))
            `fails with` ("A single account must be consumed")
        }
    }

    @Test
    fun `The supplier has not signed the transaction`() {
        ledgerServices.transaction() {
            input(AccountContract.ID, defaultState.copy(supplier = ukPower.party))
            command(defaultSigners, AccountContract.Commands.Delete())
            `fails with` ("All participants have signed the transaction")
        }
    }

    @Test
    fun `The regulator has not signed the transaction`() {
        ledgerServices.transaction() {
            input(AccountContract.ID, defaultState)
            command(listOf(britishEnergy.publicKey), AccountContract.Commands.Delete())
            `fails with` ("The regulator has signed the transaction")
        }
    }

    @Test
    fun `Valid delete transaction`() {
        ledgerServices.transaction() {
            input(AccountContract.ID, defaultState)
            command(defaultSigners, AccountContract.Commands.Delete())
            verifies()
        }
    }
}