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
import net.corda.examples.energyaccount.contracts.CustomerDetails
import net.corda.examples.energyaccount.flows.BillingEntryAccountFlowInitiator
import net.corda.node.services.Permissions
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.InProcess
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User
import org.hamcrest.MatcherAssert
import org.hamcrest.number.BigDecimalCloseTo
import org.hamcrest.text.StringContainsInOrder
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
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

    private val customerA = CustomerDetails(
            "Alice",
            "Anderson",
            LocalDate.parse("1980-01-01"),
            "1 London Wall")

    private val customerB = CustomerDetails(
            "Bob",
            "Benson",
            LocalDate.parse("1981-02-02"),
            "2 Old Jewry",
            "01234 567890",
            "bob.benson@gmail.com")

    private val customerC = CustomerDetails(
            "Charlie",
            "Chapman",
            LocalDate.parse("1982-10-20"),
            "3 Poultry Lane",
            "01234 567890",
            "charlie.chapman@live.com")

    private val EPSILON = BigDecimal(1E-10)

    @Test
    fun `Account integration tests - create, modify, transfer, delete`() {
        val user = User("user1", "test", setOf(Permissions.all()))
        driver(DriverParameters(
                startNodesInProcess = true,
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
            val accountAId = supplierACli.createAccount(customerA).linearId
            log.info("Created account A with ID $accountAId")

            val accountBId = supplierBCli.createAccount(customerB).linearId
            log.info("Created account B with ID $accountBId")

            // Confirm that the supplier knows about only their owned accounts, and the regulator
            // knows about both
            var (supplierA_A, supplierB_A, regulator_A) =
                    listOf(supplierACli, supplierBCli, regulatorCli)
                            .map { it.getAccountByLinearId(accountAId) }

            var (supplierA_B, supplierB_B, regulator_B) =
                    listOf(supplierACli, supplierBCli, regulatorCli)
                            .map { it.getAccountByLinearId(accountBId) }

            with (supplierA_A!!.customerDetails) {
                assertThat(firstName, equalTo("Alice"))
                assertThat(lastName, equalTo("Anderson"))
            }

            assertNull(supplierB_A)

            with (regulator_A!!.customerDetails) {
                assertThat(firstName, equalTo("Alice"))
                assertThat(lastName, equalTo("Anderson"))
            }

            assertNull(supplierA_B)

            with (supplierB_B!!.customerDetails) {
                assertThat(firstName, equalTo("Bob"))
                assertThat(lastName, equalTo("Benson"))
            }

            with (regulator_B!!.customerDetails) {
                assertThat(firstName, equalTo("Bob"))
                assertThat(lastName, equalTo("Benson"))
            }

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
                        StringContainsInOrder(listOf(
                                "a required signer, but no session has been passed in for them")))
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
            val accountCId = supplierACli.createAccount(customerC).linearId
            val supplierAIdentity = supplierA.nodeInfo.legalIdentities[0]
            val supplierBIdentity = supplierB.nodeInfo.legalIdentities[0]

            log.info("Created account C with ID $accountBId")

            val (supplierA_C, supplierB_C, regulator_C) =
                    listOf(supplierACli, supplierBCli, regulatorCli)
                            .map { it.getAccountByLinearId(accountCId) }

            with (supplierA_C!!.customerDetails) {
                assertThat(firstName, equalTo("Charlie"))
                assertThat(lastName, equalTo("Chapman"))
            }

            assertThat(supplierA_C.supplier, equalTo(supplierAIdentity))

            assertNull(supplierB_C)

            with (regulator_C!!.customerDetails) {
                assertThat(firstName, equalTo("Charlie"))
                assertThat(lastName, equalTo("Chapman"))
            }

            assertThat(regulator_C.supplier, equalTo(supplierAIdentity))

            // Transfer account to supplier B
            supplierACli.transferAccount(accountCId, supplierBIdentity)

            // TODO: This is getting a bit out of hand, need to revisit supplier var storage
            val (supplierA_C_2, supplierB_C_2, regulator_C_2) =
                    listOf(supplierACli, supplierBCli, regulatorCli)
                            .map { it.getAccountByLinearId(accountCId) }

            assertNull(supplierA_C_2)

            with (supplierB_C_2!!.customerDetails) {
                assertThat(firstName, equalTo("Charlie"))
                assertThat(lastName, equalTo("Chapman"))
            }

            assertThat(supplierB_C_2.supplier, equalTo(supplierBIdentity))

            with (supplierB_C_2.customerDetails) {
                assertThat(firstName, equalTo("Charlie"))
                assertThat(lastName, equalTo("Chapman"))
            }

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
                        StringContainsInOrder(listOf(
                                "Failed requirement: The old and new supplier must differ")))
            }

            // Attempt transfer back from B->A - expected success
            supplierBCli.transferAccount(accountCId, supplierAIdentity)

            val (supplierA_C_3, supplierB_C_3, regulator_C_3) =
                    listOf(supplierACli, supplierBCli, regulatorCli)
                            .map { it.getAccountByLinearId(accountCId) }

            with (supplierA_C_3!!.customerDetails) {
                assertThat(firstName, equalTo("Charlie"))
                assertThat(lastName, equalTo("Chapman"))
            }

            assertThat(supplierA_C_3.supplier, equalTo(supplierAIdentity))

            assertNull(supplierB_C_3)

            with (supplierA_C_3.customerDetails) {
                assertThat(firstName, equalTo("Charlie"))
                assertThat(lastName, equalTo("Chapman"))
            }

            assertThat(regulator_C_3?.supplier, equalTo(supplierAIdentity))

            // Modify account C
            supplierACli.modifyAccount(accountCId, customerC.copy(email = "newemail@gmail.com"))

            val (supplierA_C_4, supplierB_C_4, regulator_C_4) =
                    listOf(supplierACli, supplierBCli, regulatorCli)
                            .map { it.getAccountByLinearId(accountCId) }

            assertThat(supplierA_C_4?.customerDetails?.email, equalTo("newemail@gmail.com"))
            assertNull(supplierB_C_4)
            assertThat(regulator_C_4?.customerDetails?.email, equalTo("newemail@gmail.com"))

            // Attempt transfer of deleted account - expected failure
            with(assertFailsWith<FlowException> {
                supplierBCli.transferAccount(accountBId, supplierAIdentity) }) {

                MatcherAssert.assertThat(
                        this.message,
                        StringContainsInOrder(listOf("Account with id ", " not found.")))
            }
        }
    }

    @Test
    fun `Account integration tests - meter readings and billing`() {
        val user = User("user1", "test", setOf(Permissions.all()))
        driver(DriverParameters(
                startNodesInProcess = true,
                isDebug = true,
                cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("net.corda.examples.energyaccount.contracts"),
                        TestCordapp.findCordapp("net.corda.examples.energyaccount.flows"))
        )) {
            val (regulator, supplierA) = listOf(
                    startNode(providedName = regulatorName, rpcUsers = listOf(user)),
                    startNode(providedName = supplierAName, rpcUsers = listOf(user))
            ).map { (it.getOrThrow() as InProcess) }

            val (regulatorCli, supplierACli) =
                    listOf(regulator, supplierA).map {
                        val client = CordaRPCClient(it.rpcAddress)
                        client.start(user.username, user.password)
                        val api = AccountClientApi()
                        api.rpc = it.rpc
                        api
                    }

            // Create account
            val accountAId = supplierACli.createAccount(customerA).linearId
            log.info("Created account A with ID $accountAId")

            var supplierA_A = supplierACli.getAccountByLinearId(accountAId)

            // Create bill prior to meter reading
            assertThat(supplierA_A?.billingEntries?.size, equalTo(0))
            supplierACli.billingEntry(
                    accountAId,
                    BillingEntryAccountFlowInitiator.Companion.EntryType.STANDARD,
                    null,
                    null,
                    LocalDateTime.parse("2018-12-03T10:15:29"))

            supplierA_A = supplierACli.getAccountByLinearId(accountAId)
            var regulator_A = regulatorCli.getAccountByLinearId(accountAId)

            assertThat(supplierA_A?.billingEntries?.size, equalTo(1))
            assertThat(
                    supplierA_A?.billingEntries?.get(0)?.eventDT.toString(),
                    equalTo("2018-12-03T10:15:29"))
            assertThat(supplierA_A?.billingEntries?.get(0)?.amount, equalTo(BigDecimal(0)))
            assertThat(supplierA_A?.billingEntries?.get(0)?.balance, equalTo(BigDecimal(0)))

            assertThat(
                    regulator_A?.billingEntries?.hashCode(),
                    equalTo(supplierA_A?.billingEntries?.hashCode()))

            // Submit first meter reading for account A
            assertThat(supplierA_A?.meterReadings?.size, equalTo(0))
            supplierACli.meterRead(
                    accountAId,
                    1000,
                    LocalDateTime.parse("2018-12-03T10:15:30"))

            supplierA_A = supplierACli.getAccountByLinearId(accountAId)
            regulator_A = regulatorCli.getAccountByLinearId(accountAId)

            assertThat(supplierA_A?.meterReadings?.size, equalTo(1))
            assertThat(
                    supplierA_A?.meterReadings?.get(0)?.first.toString(),
                    equalTo("2018-12-03T10:15:30"))
            assertThat(supplierA_A?.meterReadings?.get(0)?.second, equalTo(1000))

            assertThat(
                    regulator_A?.meterReadings?.hashCode(),
                    equalTo(supplierA_A?.meterReadings?.hashCode()))

            // Create bill following first reading
            supplierACli.billingEntry(
                    accountAId,
                    BillingEntryAccountFlowInitiator.Companion.EntryType.STANDARD,
                    null,
                    null,
                    LocalDateTime.parse("2018-12-03T11:15:29"))

            supplierA_A = supplierACli.getAccountByLinearId(accountAId)
            regulator_A = regulatorCli.getAccountByLinearId(accountAId)

            // 1000 units at £0.15 per unit
            var thisBillAmount = 1000 * 0.15

            assertThat(supplierA_A?.billingEntries?.size, equalTo(2))
            assertThat(
                    supplierA_A?.billingEntries?.get(1)?.eventDT.toString(),
                    equalTo("2018-12-03T11:15:29"))
            MatcherAssert.assertThat(
                    supplierA_A?.billingEntries?.get(1)?.amount,
                    BigDecimalCloseTo(BigDecimal(thisBillAmount), EPSILON))
            MatcherAssert.assertThat(
                    supplierA_A?.billingEntries?.get(1)?.balance,
                    BigDecimalCloseTo(BigDecimal(thisBillAmount), EPSILON))

            assertThat(
                    regulator_A?.billingEntries?.hashCode(),
                    equalTo(supplierA_A?.billingEntries?.hashCode()))

            // Submit second meter reading for account A
            supplierACli.meterRead(
                    accountAId,
                    1004,
                    LocalDateTime.parse("2018-12-03T11:15:30"))

            supplierA_A = supplierACli.getAccountByLinearId(accountAId)
            regulator_A = regulatorCli.getAccountByLinearId(accountAId)

            assertThat(supplierA_A?.meterReadings?.size, equalTo(2))
            assertThat(
                    supplierA_A?.meterReadings?.get(0)?.first.toString(),
                    equalTo("2018-12-03T10:15:30"))
            assertThat(supplierA_A?.meterReadings?.get(0)?.second, equalTo(1000))
            assertThat(
                    supplierA_A?.meterReadings?.get(1)?.first.toString(),
                    equalTo("2018-12-03T11:15:30"))
            assertThat(supplierA_A?.meterReadings?.get(1)?.second, equalTo(1004))

            assertThat(
                    regulator_A?.meterReadings?.hashCode(),
                    equalTo(supplierA_A?.meterReadings?.hashCode()))

            // Create bill following second reading
            supplierACli.billingEntry(
                    accountAId,
                    BillingEntryAccountFlowInitiator.Companion.EntryType.STANDARD,
                    null,
                    null,
                    LocalDateTime.parse("2018-12-03T12:00:00"))

            supplierA_A = supplierACli.getAccountByLinearId(accountAId)
            regulator_A = regulatorCli.getAccountByLinearId(accountAId)

            // Delta of 4 units units at £0.15 per unit
            thisBillAmount = 4 * 0.15

            assertThat(supplierA_A?.billingEntries?.size, equalTo(3))
            assertThat(
                    supplierA_A?.billingEntries?.get(2)?.eventDT.toString(),
                    equalTo("2018-12-03T12:00"))
            MatcherAssert.assertThat(
                    supplierA_A?.billingEntries?.get(2)?.amount,
                    BigDecimalCloseTo(BigDecimal(thisBillAmount), EPSILON))
            MatcherAssert.assertThat(
                    supplierA_A?.billingEntries?.get(2)?.balance,
                    BigDecimalCloseTo(BigDecimal(150 + thisBillAmount), EPSILON))

            assertThat(
                    regulator_A?.billingEntries?.hashCode(),
                    equalTo(supplierA_A?.billingEntries?.hashCode()))

            // Invalid meter reading - previous date
            with(assertFailsWith<FlowException> {
                supplierACli.meterRead(
                        accountAId,
                        1005,
                        LocalDateTime.parse("2018-11-03T11:15:30")) }) {

                MatcherAssert.assertThat(
                        this.message,
                        StringContainsInOrder(listOf(
                                "Failed requirement: The time of the " +
                                        "new reading must be greater than the previous reading")))
            }

            // Invalid meter reading - previous value
            with(assertFailsWith<FlowException> {
                supplierACli.meterRead(
                        accountAId,
                        1004,
                        LocalDateTime.parse("2018-12-04T12:15:30")) }) {

                MatcherAssert.assertThat(
                        this.message,
                        StringContainsInOrder(listOf(
                                "Failed requirement: The value of the " +
                                        "new reading must be greater than the previous reading")))
            }

            // Create two new readings, then bill
            supplierACli.meterRead(accountAId, 1006, null)
            supplierACli.meterRead(accountAId, 1007, null)
            supplierACli.billingEntry(
                    accountAId,
                    BillingEntryAccountFlowInitiator.Companion.EntryType.STANDARD,
                    null,
                    null,
                    null)

            supplierA_A = supplierACli.getAccountByLinearId(accountAId)
            regulator_A = regulatorCli.getAccountByLinearId(accountAId)

            // Delta of 3 units units at £0.15 per unit
            thisBillAmount = 3 * 0.15

            assertThat(supplierA_A?.billingEntries?.size, equalTo(4))
            MatcherAssert.assertThat(
                    supplierA_A?.billingEntries?.get(3)?.amount,
                    BigDecimalCloseTo(BigDecimal(thisBillAmount), EPSILON))
            MatcherAssert.assertThat(
                    supplierA_A?.billingEntries?.get(3)?.balance,
                    BigDecimalCloseTo(BigDecimal(150.6 + thisBillAmount), EPSILON))

            assertThat(
                    regulator_A?.billingEntries?.hashCode(),
                    equalTo(supplierA_A?.billingEntries?.hashCode()))

            // Create a bill with no further readings
            supplierACli.billingEntry(
                    accountAId,
                    BillingEntryAccountFlowInitiator.Companion.EntryType.STANDARD,
                    null,
                    null,
                    null)

            supplierA_A = supplierACli.getAccountByLinearId(accountAId)
            regulator_A = regulatorCli.getAccountByLinearId(accountAId)

            assertThat(supplierA_A?.billingEntries?.size, equalTo(5))
            MatcherAssert.assertThat(
                    supplierA_A?.billingEntries?.get(4)?.amount,
                    BigDecimalCloseTo(BigDecimal(0), EPSILON))
            MatcherAssert.assertThat(
                    supplierA_A?.billingEntries?.get(4)?.balance,
                    BigDecimalCloseTo(BigDecimal(151.05), EPSILON))

            assertThat(
                    regulator_A?.billingEntries?.hashCode(),
                    equalTo(supplierA_A?.billingEntries?.hashCode()))

            // Submit bill adjustment
            supplierACli.billingEntry(
                    accountAId,
                    BillingEntryAccountFlowInitiator.Companion.EntryType.ADJUST,
                    "Adjustment",
                    BigDecimal(2.50),
                    null)

            supplierA_A = supplierACli.getAccountByLinearId(accountAId)
            regulator_A = regulatorCli.getAccountByLinearId(accountAId)

            assertThat(supplierA_A?.billingEntries?.size, equalTo(6))

            assertThat(
                    supplierA_A?.billingEntries?.get(5)?.eventDescription,
                    equalTo("Adjustment"))
            MatcherAssert.assertThat(
                    supplierA_A?.billingEntries?.get(5)?.amount,
                    BigDecimalCloseTo(BigDecimal(2.50), EPSILON))
            MatcherAssert.assertThat(
                    supplierA_A?.billingEntries?.get(5)?.balance,
                    BigDecimalCloseTo(BigDecimal(153.55), EPSILON))

            assertThat(
                    regulator_A?.billingEntries?.hashCode(),
                    equalTo(supplierA_A?.billingEntries?.hashCode()))

            // Make payment against account
            supplierACli.billingEntry(
                    accountAId,
                    BillingEntryAccountFlowInitiator.Companion.EntryType.ADJUST,
                    "Payment",
                    BigDecimal(-30.00),
                    null)

            supplierA_A = supplierACli.getAccountByLinearId(accountAId)
            regulator_A = regulatorCli.getAccountByLinearId(accountAId)

            assertThat(supplierA_A?.billingEntries?.size, equalTo(7))

            assertThat(
                    supplierA_A?.billingEntries?.get(6)?.eventDescription,
                    equalTo("Payment"))
            MatcherAssert.assertThat(
                    supplierA_A?.billingEntries?.get(6)?.amount,
                    BigDecimalCloseTo(BigDecimal(-30.0), EPSILON))
            MatcherAssert.assertThat(
                    supplierA_A?.billingEntries?.get(6)?.balance,
                    BigDecimalCloseTo(BigDecimal(123.55), EPSILON))

            assertThat(
                    regulator_A?.billingEntries?.hashCode(),
                    equalTo(supplierA_A?.billingEntries?.hashCode()))

            // Invalid billing entry - previous date
            with(assertFailsWith<FlowException> {
                supplierACli.billingEntry(
                        accountAId,
                        BillingEntryAccountFlowInitiator.Companion.EntryType.ADJUST,
                        "Payment",
                        BigDecimal(-30.00),
                        LocalDateTime.parse("2018-12-02T10:15:30")) }) {

                MatcherAssert.assertThat(
                        this.message,
                        StringContainsInOrder(listOf(
                                "Failed requirement: The time of the " +
                                        "new entry must be greater than the previous entry")))
            }
        }
    }
}
