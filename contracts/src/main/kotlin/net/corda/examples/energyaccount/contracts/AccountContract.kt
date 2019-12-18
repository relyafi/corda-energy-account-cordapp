package net.corda.examples.energyaccount.contracts

import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import java.security.PublicKey

class AccountContract : Contract {

    interface Commands : CommandData {
        class Create: Commands, TypeOnlyCommandData()
        class Transfer: Commands, TypeOnlyCommandData()
        class Delete: Commands, TypeOnlyCommandData()
    }

    companion object {
        const val ID = "net.corda.examples.energyaccount.contracts.AccountContract"

        fun generateAccountCreate(
                notary: Party,
                regulator: Party,
                supplier: Party,
                firstName: String,
                lastName: String) : TransactionBuilder {

            return TransactionBuilder(notary)
                    .addOutputState(AccountState(regulator, supplier, firstName, lastName))
                    .addCommand(Command(Commands.Create(), listOf(
                            regulator.owningKey,
                            supplier.owningKey)))
        }

        fun generateAccountTransfer(
                notary: Party,
                account: StateAndRef<AccountState>,
                newSupplier: Party) : TransactionBuilder {
            return TransactionBuilder(notary)
                    .addInputState(account)
                    .addOutputState(account.state.data.withNewSupplier(newSupplier))
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
    }

    override fun verify(tx: LedgerTransaction) {
        val cmd = tx.commands.requireSingleCommand<AccountContract.Commands>()
        val signers = cmd.signers.toSet()

        when (cmd.value) {
            is Commands.Create -> validateCreate(tx, signers)
            is Commands.Transfer -> validateTransfer(tx, signers)
            is Commands.Delete -> validateDelete(tx, signers)
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

    private fun validateMandatoryAccountFields(account: AccountState) = requireThat {
        "Name is populated" using (account.firstName.isNotEmpty() && account.lastName.isNotEmpty())
    }
}
