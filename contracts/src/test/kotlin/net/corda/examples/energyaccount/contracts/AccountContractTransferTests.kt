package net.corda.examples.energyaccount.contracts

import net.corda.testing.node.transaction
import org.junit.Test

class AccountContractTransferTests : AccountContractTestBase() {

    @Test
    fun `No input state provided`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.Transfer())
            output(AccountContract.ID, defaultState)
            `fails with` ("A single account must be consumed")
        }
    }

    @Test
    fun `Wrong number of output states provided`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.Transfer())
            `fails with` ("A transaction must contain at least one input or output state")
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState.copy(supplier = ukPower.party))
            `fails with` ("A single account must be created")
        }
    }

    @Test
    fun `The new supplier has not signed the transaction`() {
        ledgerServices.transaction() {
            command(listOf(britishEnergy.publicKey, regulator.publicKey),
                    AccountContract.Commands.Transfer())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState.copy(supplier = ukPower.party))
            `fails with` ("All participants have signed the transaction")
        }
    }

    @Test
    fun `The supplier has not changed`() {
        ledgerServices.transaction() {
            command(listOf(britishEnergy.publicKey, ukPower.publicKey, regulator.publicKey),
                    AccountContract.Commands.Transfer())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState.copy(supplier = britishEnergy.party))
            `fails with` ("The old and new supplier must differ")
        }
    }

    @Test
    fun `Customer details have changed`() {
        ledgerServices.transaction() {
            command(listOf(britishEnergy.publicKey, ukPower.publicKey, regulator.publicKey),
                    AccountContract.Commands.Transfer())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID,
                   defaultState.copy(
                           supplier = ukPower.party,
                           customerDetails = defaultState.customerDetails.copy(
                                    address = "2, Moorgate")))
            `fails with`("Customer details must be the same")
        }
    }

    @Test
    fun `Valid  transfer transaction`() {
        ledgerServices.transaction() {
            command(listOf(britishEnergy.publicKey, ukPower.publicKey, regulator.publicKey),
                    AccountContract.Commands.Transfer())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID,
                    defaultState.copy(supplier = ukPower.party))
            verifies()
        }
    }
}