package net.corda.examples.energyaccount.contracts

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Encapsulates a single bill entry against an account
 */
@CordaSerializable
data class BillingEntry(val supplier: Party,
                        val eventDT: LocalDateTime,
                        val eventDescription: String,
                        val amount: BigDecimal,
                        val balance: BigDecimal) {
}