package net.corda.examples.energyaccount.contracts

import net.corda.testing.node.transaction
import org.junit.Test

class AccountContractCreateTests : AccountContractTestBase() {

    @Test
    fun `Input state provided`() {
        ledgerServices.transaction() {
            input(AccountContract.ID, defaultState)
            command(defaultSigners, AccountContract.Commands.Create())
            output(AccountContract.ID, defaultState)
            `fails with` ("No inputs should be consumed")
        }
    }

    @Test
    fun `Wrong number of output states provided`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.Create())
            `fails with` ("A transaction must contain at least one input or output state")
            output(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState.withNewName("Alice", "Anderson"))
            `fails with` ("A single account must be created")
        }
    }

    @Test
    fun `The supplier has not signed the transaction`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.Create())
            output(AccountContract.ID, defaultState.withNewSupplier(ukPower.party))
            `fails with` ("All participants have signed the transaction")
        }
    }

    @Test
    fun `The regulator has not signed the transaction`() {
        ledgerServices.transaction() {
            command(listOf(britishEnergy.publicKey), AccountContract.Commands.Create())
            output(AccountContract.ID, defaultState)
            `fails with` ("The regulator has signed the transaction")
        }
    }

    @Test
    fun `Mandatory fields have not been populated`() {
        // TODO: See if we can make this a proper JUnit parameterised test
        for ((firstName, lastName) in listOf(
                Pair("", ""),
                Pair("Joe", ""),
                Pair("", "Blogs"))) {
            ledgerServices.transaction() {
                command(defaultSigners, AccountContract.Commands.Create())
                output(AccountContract.ID, defaultState.withNewName(firstName, lastName))
                `fails with` ("Name is populated")
            }
        }
    }

    @Test
    fun `Valid create transaction`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.Create())
            output(AccountContract.ID, defaultState)
            verifies()
        }
    }
}