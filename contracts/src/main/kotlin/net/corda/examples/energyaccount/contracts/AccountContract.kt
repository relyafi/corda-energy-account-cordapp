package net.corda.examples.energyaccount.contracts

import net.corda.examples.energyaccount.contracts.AccountState
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import java.security.PublicKey

class AccountContract : Contract {

    interface Commands : CommandData {
        class Create: Commands, TypeOnlyCommandData()
        class Destroy: Commands, TypeOnlyCommandData()
    }

    companion object {
        const val ID = "net.corda.examples.energyaccount.contracts.AccountContract"
    }

    fun generateAccountCreate(
            notary: Party,
            supplier: AbstractParty,
            firstName: String,
            lastName: String) : TransactionBuilder {

        return TransactionBuilder(notary)
                .addOutputState(AccountState(supplier, firstName, lastName))
                .addCommand(Command(Commands.Create(), supplier.owningKey))
    }

    fun generateAccountDestroy(
            notary: Party,
            account: StateAndRef<AccountState>) : TransactionBuilder {
        return TransactionBuilder(notary)
                .addInputState(account)
                .addCommand(Command(Commands.Destroy(), account.state.data.supplier.owningKey))
    }

    override fun verify(tx: LedgerTransaction) {
        val cmd = tx.commands.requireSingleCommand<AccountContract.Commands>()
        val signers = cmd.signers.toSet()

        when (cmd.value) {
            is Commands.Create -> validateCreate(tx, signers)
            is Commands.Destroy -> validateDestroy(tx, signers)
            else -> throw IllegalArgumentException("unrecognised command")
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
                    (signers == keysFromParticipants(outState))
        }
    }

    private fun validateDestroy(tx: LedgerTransaction, signers: Set<PublicKey>) {

        requireThat {
            "No outputs should be created" using tx.outputs.isEmpty()
            "A single account must be consumed" using  (tx.inputs.size == 1)
            "All participants have signed the transaction" using
                    (signers == keysFromParticipants(tx.inputsOfType<AccountState>().single()))
        }
    }

    private fun validateMandatoryAccountFields(account: AccountState) = requireThat {
        "Name is populated" using (account.firstName.isNotEmpty() && account.lastName.isNotEmpty())
    }
}