package net.corda.examples.energyaccount.contracts

import net.corda.testing.node.transaction
import org.junit.Test
import java.math.BigDecimal

class AccountContractBillingTests : AccountContractTestBase() {

    @Test
    fun `No input state provided`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            output(AccountContract.ID, defaultState)
            `fails with` ("A single account must be consumed")
        }
    }

    @Test
    fun `Wrong number of output states provided`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
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
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState.copy(supplier = ukPower.party))
            `fails with` ("The old and new supplier must be the same")
        }
    }

    @Test
    fun `Customer details have changed`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID,
                   defaultState.copy(
                           customerDetails = defaultState.customerDetails.copy(
                                    address = "2, Moorgate")))
            `fails with`("Customer details must be the same")
        }
    }

    @Test
    fun `Multiple new billing entries`() {
        val initialState = defaultState.withNewReading(1, nextDT())
        val newState = initialState
                .withNewBillingEntry("Event 1", BigDecimal(20.00), nextDT())
                .withNewBillingEntry("Event 2", BigDecimal(20.00), nextDT())

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            `fails with`( "There must be exactly one more entry")
        }
    }

    @Test
    fun `Entry time is lower than previous entry`() {
        val initialState = defaultState
                .withNewBillingEntry("Event 1", BigDecimal(20.00), nextDT())
        val newState = initialState
                .withNewBillingEntry(
                        "Event 2",
                        BigDecimal(20.00),
                        nextDT().minusHours(1))

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            `fails with`( "The time of the new entry must be greater than the previous entry")
        }
    }

    @Test
    fun `Different billing histories`() {
        val DT1 = nextDT();
        val DT2 = nextDT();

        val initialState = defaultState
                .withNewBillingEntry("Event 1", BigDecimal(20.00), DT1)
                .withNewBillingEntry("Event 2", BigDecimal(-10.00), DT2)
        val newState = defaultState
                .withNewBillingEntry("Event 1", BigDecimal(20.01), DT1)
                .withNewBillingEntry("Event 2", BigDecimal(-10.00), DT2)
                .withNewBillingEntry("Event 3", BigDecimal(5.50), nextDT())

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            `fails with`( "The billing history must be the same")
        }
    }

    @Test
    fun `Valid billing transaction - first value, positive`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState
                    .withNewBillingEntry("Event 1", BigDecimal(20.00), nextDT()))
            verifies()
        }
    }

    @Test
    fun `Valid billing transaction - first value, negative`() {
        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, defaultState)
            output(AccountContract.ID, defaultState
                    .withNewBillingEntry("Event 1", BigDecimal(-10.00), nextDT()))
            verifies()
        }
    }

    // TODO: Remaining tests are candidates for parameterised tests
    @Test
    fun `Valid billing transaction - subsequent value, positive, increment, no flip`() {
        val initialState = defaultState
                .withNewBillingEntry("Event 1", BigDecimal(20.00), nextDT())
        val newState = initialState
                .withNewBillingEntry("Event 2", BigDecimal(5.00), nextDT())

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            verifies()
        }
    }

    @Test
    fun `Valid billing transaction - subsequent value, positive, decrement, no flip`() {
        val initialState = defaultState
                .withNewBillingEntry("Event 1", BigDecimal(20.00), nextDT())
        val newState = initialState
                .withNewBillingEntry("Event 2", BigDecimal(-5.00), nextDT())

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            verifies()
        }
    }

    @Test
    fun `Valid billing transaction - subsequent value, positive, decrement, flip`() {
        val initialState = defaultState
                .withNewBillingEntry("Event 1", BigDecimal(20.00), nextDT())
        val newState = initialState
                .withNewBillingEntry("Event 2", BigDecimal(-25.00), nextDT())

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            verifies()
        }
    }

    @Test
    fun `Valid billing transaction - subsequent value, negative, increment, no flip`() {
        val initialState = defaultState
                .withNewBillingEntry("Event 1", BigDecimal(-20.00), nextDT())
        val newState = initialState
                .withNewBillingEntry("Event 2", BigDecimal(5.00), nextDT())

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            verifies()
        }
    }

    @Test
    fun `Valid billing transaction - subsequent value, negatiive, increment, flip`() {
        val initialState = defaultState
                .withNewBillingEntry("Event 1", BigDecimal(-20.00), nextDT())
        val newState = initialState
                .withNewBillingEntry("Event 2", BigDecimal(25.00), nextDT())

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            verifies()
        }
    }

    @Test
    fun `Valid billing transaction - subsequent value, negative, decrement, no flip`() {
        val initialState = defaultState
                .withNewBillingEntry("Event 1", BigDecimal(-20.00), nextDT())
        val newState = initialState
                .withNewBillingEntry("Event 2", BigDecimal(-25.00), nextDT())

        ledgerServices.transaction() {
            command(defaultSigners, AccountContract.Commands.BillingEntry())
            input(AccountContract.ID, initialState)
            output(AccountContract.ID, newState)
            verifies()
        }
    }
}