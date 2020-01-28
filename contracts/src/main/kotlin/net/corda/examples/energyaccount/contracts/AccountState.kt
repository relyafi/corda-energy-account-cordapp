package net.corda.examples.energyaccount.contracts

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Encapsulates a single customers account at a point in time. A unique identifier
 * servers as the linear id of an account so that previously spent "versions" of the
 * account can be retrieved.
 */
@BelongsToContract(AccountContract::class)
data class AccountState(
        val regulator: Party,
        val supplier: Party,
        val customerDetails: CustomerDetails,
        val meterReadings: List<Pair<LocalDateTime, Int>> = listOf(),
        val billingEntries: List<BillingEntry> = listOf(),
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> = listOf(supplier)

    /**
     * Generates a new meter reading. The dateTime argument defaults to now if not
     * supplied.
     */
    fun withNewReading(units : Int,
                       dateTime: LocalDateTime? = null) : AccountState {

        return this.copy(meterReadings =
                this.meterReadings + Pair(dateTime ?: LocalDateTime.now(), units))
    }

    /**
     * Generates a new billing entry. The dateTime argument defaults to now if not
     * supplied.
     */
    fun withNewBillingEntry(description: String,
                            amount: BigDecimal,
                            dateTime: LocalDateTime? = null) : AccountState {
        val newEntry = BillingEntry(
                this.supplier,
                dateTime ?: LocalDateTime.now(),
                description,
                amount,
                if (this.billingEntries.isEmpty())
                    amount
                else
                    this.billingEntries.last().balance + amount)
        return this.copy(billingEntries = this.billingEntries + newEntry)
    }
}
