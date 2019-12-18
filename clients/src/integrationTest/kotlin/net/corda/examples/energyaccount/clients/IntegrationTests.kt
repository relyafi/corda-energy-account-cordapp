package net.corda.examples.energyaccount.clients

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.CordaRuntimeException
import net.corda.core.flows.FlowException
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.getOrThrow
import net.corda.examples.energyaccount.clients.api.AccountClientApi
import net.corda.node.services.Permissions
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.InProcess
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User
import org.hamcrest.MatcherAssert
import org.hamcrest.core.StringContains
import org.hamcrest.text.StringContainsInOrder
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class IntegrationTests {

    private companion object {
        private val log = contextLogger()
    }

    private val regulatorName =
            CordaX500Name("Government Regulator", "London", "GB")
    private val supplierAName =
            CordaX500Name("British Energy", "Manchester", "GB")
    private val supplierBName =
            CordaX500Name("UK Power", "Newcastle", "GB")

    @Test
    fun `Account integration tests`() {
        val user = User("user1", "test", setOf(Permissions.all()))
        driver(DriverParameters(
                startNodesInProcess = true,
                isDebug = true,
                cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("net.corda.examples.energyaccount.contracts"),
                        TestCordapp.findCordapp("net.corda.examples.energyaccount.flows"))
        )) {
            val (regulator, supplierA, supplierB) = listOf(
                    startNode(providedName = regulatorName, rpcUsers = listOf(user)),
                    startNode(providedName = supplierAName, rpcUsers = listOf(user)),
                    startNode(providedName = supplierBName, rpcUsers = listOf(user))
            ).map { (it.getOrThrow() as InProcess) }

            val (regulatorCli, supplierACli, supplierBCli) =
                    listOf(regulator, supplierA, supplierB).map {
                        val client = CordaRPCClient(it.rpcAddress)
                        client.start(user.username, user.password)
                        val api = AccountClientApi()
                        api.rpc = it.rpc
                        api
            }

            // Create two accounts, one on each supplier
            val accountAId = supplierACli.createAccount("Alice", "Anderson").linearId
            log.info("Created account A with ID $accountAId")

            val accountBId = supplierBCli.createAccount("Bob", "Benson").linearId
            log.info("Created account B with ID $accountBId")

            // Confirm that the supplier knows about only their owned accounts, and the regulator
            // knows about both
            val (supplierA_A, supplierB_A, regulator_A) =
                    listOf(supplierACli, supplierBCli, regulatorCli)
                            .map { it.getAccountByLinearId(accountAId) }

            val (supplierA_B, supplierB_B, regulator_B) =
                    listOf(supplierACli, supplierBCli, regulatorCli)
                            .map { it.getAccountByLinearId(accountBId) }

            assertThat(supplierA_A?.firstName, equalTo("Alice"))
            assertThat(supplierA_A?.lastName, equalTo("Anderson"))
            assertNull(supplierB_A)
            assertThat(regulator_A?.firstName, equalTo("Alice"))
            assertThat(regulator_A?.lastName, equalTo("Anderson"))

            assertNull(supplierA_B)
            assertThat(supplierB_B?.firstName, equalTo("Bob"))
            assertThat(supplierB_B?.lastName, equalTo("Benson"))
            assertThat(regulator_B?.firstName, equalTo("Bob"))
            assertThat(regulator_B?.lastName, equalTo("Benson"))

            // Attempt to delete account B as regulator and supplier A - expected failure
            with(assertFailsWith<FlowException> {
                supplierACli.deleteAccount(accountBId) }) {

                MatcherAssert.assertThat(
                        this.message,
                        StringContainsInOrder(listOf("Account with id ", " not found.")))
            }

            with(assertFailsWith<CordaRuntimeException> {
                regulatorCli.deleteAccount(accountBId) }) {

                MatcherAssert.assertThat(
                        this.message,
                        StringContains(
                                "The Initiator of CollectSignaturesFlow must pass " +
                                "in exactly the sessions required to sign the transaction."))
            }

            // Delete account B as supplier B (owner)
            supplierBCli.deleteAccount(accountBId)

            // Confirm this can no longer be retrieved
            with (listOf(supplierACli, supplierBCli, regulatorCli)) {
                assertNull(this[0].getAccountByLinearId(accountBId))
                assertNull(this[1].getAccountByLinearId(accountBId))
                assertNull(this[2].getAccountByLinearId(accountBId))
            }

            // Create another new account with supplier A
            val accountCId = supplierACli.createAccount("Charlie", "Chaplin").linearId
            val supplierAIdentity = supplierA.nodeInfo.legalIdentities[0]
            val supplierBIdentity = supplierB.nodeInfo.legalIdentities[0]

            log.info("Created account C with ID $accountBId")

            val (supplierA_C, supplierB_C, regulator_C) =
                    listOf(supplierACli, supplierBCli, regulatorCli)
                            .map { it.getAccountByLinearId(accountCId) }

            assertThat(supplierA_C?.firstName, equalTo("Charlie"))
            assertThat(supplierA_C?.lastName, equalTo("Chaplin"))
            assertThat(supplierA_C?.supplier, equalTo(supplierAIdentity))
            assertNull(supplierB_C)
            assertThat(regulator_C?.firstName, equalTo("Charlie"))
            assertThat(regulator_C?.lastName, equalTo("Chaplin"))
            assertThat(regulator_C?.supplier, equalTo(supplierAIdentity))

            // Transfer account to supplier B
            supplierACli.transferAccount(accountCId, supplierBIdentity)

            // TODO: This is getting a bit out of hand, need to revisit supplier var storage
            val (supplierA_C_2, supplierB_C_2, regulator_C_2) =
                    listOf(supplierACli, supplierBCli, regulatorCli)
                            .map { it.getAccountByLinearId(accountCId) }

            assertNull(supplierA_C_2)
            assertThat(supplierB_C_2?.firstName, equalTo("Charlie"))
            assertThat(supplierB_C_2?.lastName, equalTo("Chaplin"))
            assertThat(supplierB_C_2?.supplier, equalTo(supplierBIdentity))
            assertThat(regulator_C_2?.firstName, equalTo("Charlie"))
            assertThat(regulator_C_2?.lastName, equalTo("Chaplin"))
            assertThat(regulator_C_2?.supplier, equalTo(supplierBIdentity))

            // Attempt transfer from A->B again - expected failure
            with(assertFailsWith<FlowException> {
                supplierACli.transferAccount(accountCId, supplierBIdentity) }) {

                MatcherAssert.assertThat(
                        this.message,
                        StringContainsInOrder(listOf("Account with id ", " not found.")))
            }

            // Attempt transfer from A->A - expected failure
            with(assertFailsWith<FlowException> {
                supplierACli.transferAccount(accountCId, supplierAIdentity) }) {

                MatcherAssert.assertThat(
                        this.message,
                        StringContainsInOrder(listOf("Account with id ", " not found.")))
            }

            // Attempt transfer from B->B - expected failure
            with(assertFailsWith<FlowException> {
                supplierBCli.transferAccount(accountCId, supplierBIdentity) }) {

                MatcherAssert.assertThat(
                        this.message,
                        StringContains(
                                "Failed requirement: The old and new supplier must differ"))
            }

            // Attempt transfer back from B->A - expected success
            supplierBCli.transferAccount(accountCId, supplierAIdentity)

            val (supplierA_C_3, supplierB_C_3, regulator_C_3) =
                    listOf(supplierACli, supplierBCli, regulatorCli)
                            .map { it.getAccountByLinearId(accountCId) }

            assertThat(supplierA_C_3?.firstName, equalTo("Charlie"))
            assertThat(supplierA_C_3?.lastName, equalTo("Chaplin"))
            assertThat(supplierA_C_3?.supplier, equalTo(supplierAIdentity))
            assertNull(supplierB_C_3)
            assertThat(regulator_C_3?.firstName, equalTo("Charlie"))
            assertThat(regulator_C_3?.lastName, equalTo("Chaplin"))
            assertThat(regulator_C_3?.supplier, equalTo(supplierAIdentity))

            // Attempt transfer of deleted account - expected failure
            with(assertFailsWith<FlowException> {
                supplierBCli.transferAccount(accountBId, supplierAIdentity) }) {

                MatcherAssert.assertThat(
                        this.message,
                        StringContainsInOrder(listOf("Account with id ", " not found.")))
            }
        }
    }
}
