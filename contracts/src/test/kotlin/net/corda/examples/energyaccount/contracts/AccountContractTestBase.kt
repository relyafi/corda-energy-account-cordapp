package net.corda.examples.energyaccount.contracts

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.makeTestIdentityService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

abstract class AccountContractTestBase {

    protected val ledgerServices = MockServices(
            listOf(
                    "net.corda.examples.energyaccount.contracts",
                    "net.corda.testing.contracts"
            ),
            identityService = makeTestIdentityService(),
            initialIdentity = TestIdentity(
                    CordaX500Name("TestIdentity", "", "GB")))
    protected val regulator = TestIdentity(CordaX500Name(
            "Government Regulator",
            "London",
            "GB"))
    protected val britishEnergy = TestIdentity(CordaX500Name(
            "British Energy",
            "Manchester",
            "GB"))
    protected val ukPower = TestIdentity(CordaX500Name(
            "UK Power",
            "Newcastle",
            "GB"))

    protected val defaultCustomerDetails =
            CustomerDetails("John",
                            "Smith",
                            LocalDate.parse("1980-01-01"),
                            "1, London Wall")

    protected val defaultCustomerDetails2 =
            CustomerDetails("Joe",
                    "Blogs",
                    LocalDate.parse("1999-09-09"),
                    "2, Moorgate")

    protected val defaultState = AccountState(
            regulator.party,
            britishEnergy.party,
            defaultCustomerDetails)

    protected val defaultState2 = AccountState(
            regulator.party,
            britishEnergy.party,
            defaultCustomerDetails2)

    protected val defaultSigners = listOf(regulator.publicKey, britishEnergy.publicKey)

    protected var lastReturnedDT = LocalDateTime.MIN

    // Tests run fast, so simply using now() for the DT in our tests may return
    // duplicates since returned DTs may only be to millisecond precision. This
    // function will always return the next valid unique DT to the millisecond
    // to avoid this issue.
    protected fun nextDT() : LocalDateTime {
        var nextDT = LocalDateTime.now()

        if (nextDT.isBefore(lastReturnedDT.plus(1, ChronoUnit.MILLIS))) {
            nextDT = lastReturnedDT.plus(1, ChronoUnit.MILLIS)
        }

        lastReturnedDT = nextDT

        return nextDT
    }
}