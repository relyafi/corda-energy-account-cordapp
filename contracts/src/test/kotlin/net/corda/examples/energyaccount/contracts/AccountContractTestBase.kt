package net.corda.examples.energyaccount.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.examples.energyaccount.contracts.AccountState
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
    protected val britishEnergy = TestIdentity(CordaX500Name(
            "British Energy",
            "Manchester",
            "GB"))
    protected val ukPower = TestIdentity(CordaX500Name(
            "UK Power",
            "Newcastle",
            "GB"))
    protected val ombudsman = TestIdentity(CordaX500Name(
            "Energy Ombudsman",
            "London",
            "GB"))

    protected val defaultState = AccountState(
            britishEnergy.party,
            "John",
            "Smith")
}