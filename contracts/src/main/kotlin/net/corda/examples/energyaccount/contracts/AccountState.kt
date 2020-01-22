package net.corda.examples.energyaccount.contracts

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.LocalDateTime

@BelongsToContract(AccountContract::class)
data class AccountState(
        val regulator: Party,
        val supplier: Party,
        val customerDetails: CustomerDetails,
        val meterReadings: List<Pair<LocalDateTime, Int>> = listOf(),
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> = listOf(supplier)

    fun withNewReading(units : Int,
                       dateTime: LocalDateTime? = null) : AccountState {

        return this.copy(meterReadings =
                this.meterReadings + Pair(dateTime ?: LocalDateTime.now(), units))
    }
}
