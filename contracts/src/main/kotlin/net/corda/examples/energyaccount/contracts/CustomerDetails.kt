package net.corda.examples.energyaccount.contracts

import net.corda.core.serialization.CordaSerializable
import java.time.LocalDate

/**
 * Encapsulates customer information stored against an account
 */
@CordaSerializable
data class CustomerDetails(val firstName: String,
                           val lastName: String,
                           val dateOfBirth: LocalDate,
                           val address: String,
                           val phoneNumber: String? = null,
                           val email: String? = null) {
}