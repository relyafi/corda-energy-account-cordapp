package net.corda.examples.energyaccount.contracts

import net.corda.testing.node.transaction
import org.junit.Test
import java.time.LocalDate

class AccountContractModifyTests : AccountContractTestBase() {

    @Test
    fun `No input state provided`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.Modify())
            output(AccountContract.ID, defaultState)
            `fails with` ("A single account must be consumed")
        }
    }

    @Test
    fun `Wrong number of output states provided`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.Modify())
            `fails with` ("A transaction must contain at least one input or output state")
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState)
            output(AccountContract.ID,
                   defaultState.copy(
                           customerDetails = defaultState.customerDetails.copy(
                                   firstName = "Alice",
                                   lastName = "Anderson")))
            `fails with` ("A single account must be created")
        }
    }

    @Test
    fun `The supplier has not signed the transaction`() {
        ledgerServices.transaction() {
            command(listOf(regulator.publicKey), AccountContract.Commands.Modify())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState.copy(
                    customerDetails = defaultState.customerDetails.copy(
                            address = "2, Moorgate")))
            `fails with` ("All participants have signed the transaction")
        }
    }

    @Test
    fun `The supplier has changed`() {
        ledgerServices.transaction() {
            command(listOf(regulator.publicKey), AccountContract.Commands.Modify())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState.copy(
                    supplier = ukPower.party))
            `fails with` ("The old and new supplier must be the same")
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
                command(defaultSigners, AccountContract.Commands.Modify())
                input(AccountContract.ID, defaultState)
                output(AccountContract.ID,
                       defaultState.copy(
                               customerDetails = defaultState.customerDetails.copy(
                                       firstName = firstName,
                                       lastName = lastName)))
                `fails with` ("Name is populated")
            }
        }

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.Modify())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID,
                    defaultState.copy(
                            customerDetails = defaultState.customerDetails.copy(
                                    address = "")))
            `fails with` ("Address is populated")
        }
    }

    @Test
    fun `Invalid date of birth provided`() {
        // TODO: See if we can make this a proper JUnit parameterised test
        for (dateOfBirth in listOf(LocalDate.now(), LocalDate.now().minusYears(150))) {
            ledgerServices.transaction() {
                command(defaultSigners, AccountContract.Commands.Modify())
                input(AccountContract.ID, defaultState)
                output(AccountContract.ID,
                        defaultState.copy(
                                customerDetails = defaultState.customerDetails.copy(
                                        dateOfBirth = dateOfBirth)))
                `fails with` ("Date Of Birth is valid")
            }
        }
    }

    @Test
    fun `Valid  modify transaction`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.Modify())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID,
                    defaultState.copy(
                            customerDetails = defaultState.customerDetails.copy(
                                    address = "2, Moorgate")))
            verifies()
        }
    }
}