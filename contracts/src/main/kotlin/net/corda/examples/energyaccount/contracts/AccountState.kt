package net.corda.examples.energyaccount.contracts

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.examples.energyaccount.contracts.AccountContract

@BelongsToContract(AccountContract::class)
data class AccountState(
        val regulator: Party,
        val supplier: Party,
        val customerDetails: CustomerDetails,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> = listOf(supplier)
}
