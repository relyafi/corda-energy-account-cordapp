package net.corda.examples.energyaccount.contracts

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.makeTestIdentityService

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
    protected val defaultState = AccountState(
            regulator.party,
            britishEnergy.party,
            "John",
            "Smith")

    protected val defaultSigners = listOf(regulator.publicKey, britishEnergy.publicKey)
}