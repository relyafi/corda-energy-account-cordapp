package net.corda.examples.energyaccount.contracts

import net.corda.core.serialization.CordaSerializable
import java.time.LocalDate

@CordaSerializable
data class CustomerDetails(val firstName: String,
                           val lastName: String,
                           val dateOfBirth: LocalDate,
                           val address: String,
                           val phoneNumber: String? = null,
                           val email: String? = null) {
}