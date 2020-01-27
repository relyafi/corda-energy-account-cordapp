package net.corda.examples.energyaccount.contracts

import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import java.math.BigDecimal
import java.security.PublicKey
import java.time.LocalDate
import java.time.LocalDateTime

class AccountContract : Contract {

    interface Commands : CommandData {
        class Create: Commands, TypeOnlyCommandData()
        class Modify: Commands, TypeOnlyCommandData()
        class Transfer: Commands, TypeOnlyCommandData()
        class Delete: Commands, TypeOnlyCommandData()
        class MeterRead: Commands, TypeOnlyCommandData()
        class BillingEntry: Commands, TypeOnlyCommandData()
    }

    companion object {
        const val ID = "net.corda.examples.energyaccount.contracts.AccountContract"

        fun generateAccountCreate(
                notary: Party,
                regulator: Party,
                supplier: Party,
                customerDetails: CustomerDetails) : TransactionBuilder {

            val newAccount = AccountState(regulator, supplier, customerDetails)

            return TransactionBuilder(notary)
                    .addOutputState(newAccount)
                    .addCommand(Command(Commands.Create(), listOf(
                            regulator.owningKey,
                            supplier.owningKey)))
        }

        fun generateAccountModify(
                notary: Party,
                account: StateAndRef<AccountState>,
                newCustomerDetails: CustomerDetails) : TransactionBuilder {
            return TransactionBuilder(notary)
                    .addInputState(account)
                    .addOutputState(account.state.data.copy(
                            customerDetails = newCustomerDetails))
                    .addCommand(Command(Commands.Modify(), listOf(
                            account.state.data.regulator.owningKey,
                            account.state.data.supplier.owningKey)))
        }

        fun generateAccountTransfer(
                notary: Party,
                account: StateAndRef<AccountState>,
                newSupplier: Party) : TransactionBuilder {
            return TransactionBuilder(notary)
                    .addInputState(account)
                    .addOutputState(account.state.data.copy(supplier = newSupplier))
                    .addCommand(Command(Commands.Transfer(), listOf(
                            account.state.data.regulator.owningKey,
                            account.state.data.supplier.owningKey,
                            newSupplier.owningKey)))
        }

        fun generateAccountDelete(
                notary: Party,
                account: StateAndRef<AccountState>) : TransactionBuilder {
            return TransactionBuilder(notary)
                    .addInputState(account)
                    .addCommand(Command(Commands.Delete(), listOf(
                            account.state.data.regulator.owningKey,
                            account.state.data.supplier.owningKey)))
        }

        fun generateMeterReading(
                notary: Party,
                account: StateAndRef<AccountState>,
                units: Int,
                dateTime: LocalDateTime?) : TransactionBuilder {
            return TransactionBuilder(notary)
                    .addInputState(account)
                    .addOutputState(account.state.data.withNewReading(units, dateTime))
                    .addCommand(Command(Commands.MeterRead(),
                                listOf(account.state.data.regulator.owningKey,
                                       account.state.data.supplier.owningKey)))
        }

        fun generateBillingEntry(
                notary: Party,
                account: StateAndRef<AccountState>,
                description: String,
                amount: BigDecimal,
                dateTime: LocalDateTime?) : TransactionBuilder {
            return TransactionBuilder(notary)
                    .addInputState(account)
                    .addOutputState(account.state.data.withNewBillingEntry(
                            description,
                            amount,
                            dateTime))
                    .addCommand(Command(Commands.BillingEntry(),
                            listOf(account.state.data.regulator.owningKey,
                                   account.state.data.supplier.owningKey)))
        }
    }

    override fun verify(tx: LedgerTransaction) {
        val cmd = tx.commands.requireSingleCommand<AccountContract.Commands>()
        val signers = cmd.signers.toSet()

        when (cmd.value) {
            is Commands.Create -> validateCreate(tx, signers)
            is Commands.Modify -> validateModify(tx, signers)
            is Commands.Transfer -> validateTransfer(tx, signers)
            is Commands.Delete -> validateDelete(tx, signers)
            is Commands.MeterRead -> validateMeterReading(tx, signers)
            is Commands.BillingEntry -> validateBillingEntry(tx, signers)
            else -> throw IllegalArgumentException("Unrecognised command")
        }
    }

    private fun keysFromParticipants(account: AccountState): Set<PublicKey> {
        return account.participants.map {
            it.owningKey
        }.toSet()
    }

    private fun validateCreate(tx: LedgerTransaction, signers: Set<PublicKey>) {

        requireThat {
            "No inputs should be consumed" using tx.inputs.isEmpty()
            "A single account must be created" using (tx.outputs.size == 1)
        }

        val outState = tx.outputsOfType<AccountState>().single()
        validateMandatoryAccountFields(outState)

        requireThat {
            "All participants have signed the transaction" using
                    signers.containsAll(keysFromParticipants(outState))
            "The regulator has signed the transaction" using
                    signers.contains(outState.regulator.owningKey)
            "There are no meter readings" using outState.meterReadings.isEmpty()
        }
    }

    private fun validateModify(tx: LedgerTransaction, signers: Set<PublicKey>) {
        requireThat {
            "A single account must be consumed" using (tx.inputs.size == 1)
            "A single account must be created" using (tx.outputs.size == 1)
        }

        val inState = tx.inputsOfType<AccountState>().single()
        val outState = tx.outputsOfType<AccountState>().single()

        validateMandatoryAccountFields(outState)

        requireThat {
            "The account id must be the same" using
                    inState.linearId.equals(outState.linearId)
            "The old and new supplier must be the same" using
                    inState.supplier.hashCode().equals(outState.supplier.hashCode())
            "The meter readings must be the same" using
                    inState.meterReadings.equals(outState.meterReadings)
            "All participants have signed the transaction" using
                    signers.containsAll(keysFromParticipants(outState))
        }
    }

    private fun validateTransfer(tx: LedgerTransaction, signers: Set<PublicKey>) {

        requireThat {
            "A single account must be consumed" using (tx.inputs.size == 1)
            "A single account must be created" using (tx.outputs.size == 1)
        }

        val inState = tx.inputsOfType<AccountState>().single()
        val outState = tx.outputsOfType<AccountState>().single()

        requireThat {
            "The old and new supplier must differ" using
                    !inState.supplier.hashCode().equals(outState.supplier.hashCode())
            "All participants have signed the transaction" using
                    signers.containsAll(keysFromParticipants(outState))
            "The regulator has signed the transaction" using
                    signers.contains(outState.regulator.owningKey)
            "The customer details must be the same" using
                    inState.customerDetails.equals(outState.customerDetails)
        }
    }

    private fun validateDelete(tx: LedgerTransaction, signers: Set<PublicKey>) {

        requireThat {
            "No outputs should be created" using tx.outputs.isEmpty()
            "A single account must be consumed" using (tx.inputs.size == 1)

            val inState = tx.inputsOfType<AccountState>().single()

            "All participants have signed the transaction" using
                    signers.containsAll(keysFromParticipants(inState))
            "The regulator has signed the transaction" using
                    signers.contains(inState.regulator.owningKey)
        }
    }

    private fun validateMeterReading(tx: LedgerTransaction, signers: Set<PublicKey>) {

        requireThat {
            "A single account must be consumed" using (tx.inputs.size == 1)
            "A single account must be created" using (tx.outputs.size == 1)
        }

        val inState = tx.inputsOfType<AccountState>().single()
        val outState = tx.outputsOfType<AccountState>().single()

        requireThat {
            "The account id must be the same" using
                    inState.linearId.equals(outState.linearId)
            "The old and new supplier must be the same" using
                    inState.supplier.hashCode().equals(outState.supplier.hashCode())
            "The customer details must be the same" using
                    inState.customerDetails.equals(outState.customerDetails)
            "All participants have signed the transaction" using
                    signers.containsAll(keysFromParticipants(outState))
            "There must be exactly one more reading" using
                    outState.meterReadings.size.equals(inState.meterReadings.size + 1)
        }

        if (!inState.meterReadings.isEmpty()) {
            // Validation relating to previous readings
            "The time of the new reading must be greater than the previous reading" using
                    outState.meterReadings.last().first.isAfter(
                            inState.meterReadings.last().first)

            // TODO: This should probably handle rollover, i.e. 99999->00000
            "The value of the new reading must be greater than the previous reading" using
                    (outState.meterReadings.last().second >
                            inState.meterReadings.last().second)

            "The meter reading history must be the same" using
                    inState.meterReadings.equals(
                            outState.meterReadings.subList(0, outState.meterReadings.size - 1))
        }
    }

    private fun validateBillingEntry(tx: LedgerTransaction, signers: Set<PublicKey>) {

        requireThat {
            "A single account must be consumed" using (tx.inputs.size == 1)
            "A single account must be created" using (tx.outputs.size == 1)
        }

        val inState = tx.inputsOfType<AccountState>().single()
        val outState = tx.outputsOfType<AccountState>().single()

        requireThat {
            "The account id must be the same" using
                    inState.linearId.equals(outState.linearId)
            "The old and new supplier must be the same" using
                    inState.supplier.hashCode().equals(outState.supplier.hashCode())
            "The customer details must be the same" using
                    inState.customerDetails.equals(outState.customerDetails)
            "All participants have signed the transaction" using
                    signers.containsAll(keysFromParticipants(outState))
            "There must be exactly one more entry" using
                    outState.billingEntries.size.equals(inState.billingEntries.size + 1)
        }

        if (!inState.billingEntries.isEmpty()) {
            // Validation relating to previous readings
            "The time of the new entry must be greater than the previous entry" using
                    outState.billingEntries.last().eventDT.isAfter(
                            inState.billingEntries.last().eventDT)

            "The billing history must be the same" using
                    inState.billingEntries.equals(
                            outState.billingEntries.subList(0, outState.billingEntries.size - 1))
        }
    }

    private fun validateMandatoryAccountFields(account: AccountState) = requireThat {

        "Name is populated" using (account.customerDetails.firstName.isNotEmpty() &&
                                   account.customerDetails.lastName.isNotEmpty())
        "Date Of Birth is valid" using (account.customerDetails.dateOfBirth.isBefore(
                                                LocalDate.now().minusYears(18)) &&
                                        account.customerDetails.dateOfBirth.isAfter(
                                                LocalDate.now().minusYears(130)))
        "Address is populated" using (account.customerDetails.address.isNotEmpty())
    }
}
