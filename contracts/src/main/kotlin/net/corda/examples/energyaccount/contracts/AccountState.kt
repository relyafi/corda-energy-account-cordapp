package net.corda.examples.energyaccount.contracts

import net.corda.examples.energyaccount.contracts.AccountContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(AccountContract::class)
data class AccountState(
        val supplier: AbstractParty,
        val firstName: String,
        val lastName: String) : LinearState {

    override val linearId: UniqueIdentifier = UniqueIdentifier()
    override val participants: List<AbstractParty> = listOf(supplier)

    fun withNewName(firstName: String, lastName: String) =
            copy(firstName = firstName, lastName = lastName)
    fun withNewSupplier(supplier: AbstractParty) = copy (supplier = supplier)
}
