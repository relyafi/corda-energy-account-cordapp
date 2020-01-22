package net.corda.examples.energyaccount.contracts

import net.corda.testing.node.transaction
import org.junit.Test

class AccountContractMeterReadTests : AccountContractTestBase() {

    @Test
    fun `No input state provided`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.MeterRead())
            output(AccountContract.ID, defaultState)
            `fails with` ("A single account must be consumed")
        }
    }

    @Test
    fun `Wrong number of output states provided`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.MeterRead())
            `fails with` ("A transaction must contain at least one input or output state")
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState.withNewReading(1, nextDT()))
            `fails with` ("A single account must be created")
        }
    }

    @Test
    fun `The supplier has changed`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.MeterRead())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState.copy(supplier = ukPower.party))
            `fails with` ("The old and new supplier must be the same")
        }
    }

    @Test
    fun `Customer details have changed`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.MeterRead())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID,
                   defaultState.copy(
                           customerDetails = defaultState.customerDetails.copy(
                                    address = "2, Moorgate")))
            `fails with`("Customer details must be the same")
        }
    }

    @Test
    fun `Multiple new meter readings`() {
        val initialState = defaultState.withNewReading(1, nextDT())
        val newState = initialState
                .withNewReading(2, nextDT())
                .withNewReading(4, nextDT())

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.MeterRead())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            `fails with`( "There must be exactly one more reading")
        }
    }

    @Test
    fun `Meter reading time is lower than previous reading`() {
        val initialState = defaultState.withNewReading(2, nextDT())
        val newState = initialState.withNewReading(5, nextDT().minusHours(1))

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.MeterRead())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            `fails with`( "The time of the new reading must be greater than the previous reading")
        }
    }

    @Test
    fun `Meter reading value is lower than previous reading`() {
        val initialState = defaultState.withNewReading(4, nextDT())
        val newState = initialState.withNewReading(3, nextDT())

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.MeterRead())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            `fails with`( "The value of the new reading must be greater than the previous reading")
        }
    }

    @Test
    fun `Different meter reading histories`() {
        val DT1 = nextDT();
        val DT2 = nextDT();

        val initialState = defaultState
                .withNewReading(2, DT1)
                .withNewReading(4, DT2)
        val newState = defaultState
                .withNewReading(3, DT1)
                .withNewReading(4, DT2)
                .withNewReading(7, nextDT())

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.MeterRead())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            `fails with`( "The meter reading history must be the same")
        }
    }

    @Test
    fun `Valid meter read transaction - first reading`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.MeterRead())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState.withNewReading(1, nextDT()))
            verifies()
        }
    }

    @Test
    fun `Valid meter read transaction - subsequent reading`() {

        val initialState = defaultState.withNewReading(1, nextDT())
        val newState = initialState.withNewReading(2, nextDT())

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.MeterRead())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            verifies()
        }
    }
}