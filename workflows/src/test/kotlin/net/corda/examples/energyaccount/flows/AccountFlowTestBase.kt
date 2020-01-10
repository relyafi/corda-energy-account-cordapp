package net.corda.examples.energyaccount.flows

import junit.framework.TestCase.assertEquals
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.examples.energyaccount.contracts.AccountState
import net.corda.examples.energyaccount.contracts.CustomerDetails
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import java.time.LocalDate

abstract class AccountFlowTestBase {

    lateinit var network: MockNetwork
    lateinit var regulator: StartedMockNode
    lateinit var supplierA: StartedMockNode
    lateinit var supplierB: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(
                cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("net.corda.examples.energyaccount.contracts"),
                        TestCordapp.findCordapp("net.corda.examples.energyaccount.flows")),
                threadPerNode = true))

        regulator = network.createNode(
                CordaX500Name("Government Regulator", "London","GB"))
        supplierA = network.createNode()
        supplierB = network.createNode()

        val nodes = listOf(regulator, supplierA, supplierB)

        nodes.forEach {
            it.registerInitiatedFlow(CreateAccountFlowResponder::class.java)
        }
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    protected fun createAccount(
            supplierNode: StartedMockNode,
            customerDetails: CustomerDetails) : SignedTransaction {
        val flow = CreateAccountFlowInitiator(
                regulator.info.chooseIdentity(),
                customerDetails)
        return supplierNode.startFlow(flow).getOrThrow()
    }

    protected fun transferAccount(
            supplierNode: StartedMockNode,
            accountLinearId: UniqueIdentifier,
            newSupplier: Party) : SignedTransaction {
        val flow = TransferAccountFlowInitiator(accountLinearId, newSupplier)
        return supplierNode.startFlow(flow).getOrThrow()
    }

    protected fun deleteAccount(
            supplierNode: StartedMockNode,
            accountLinearId: UniqueIdentifier) : SignedTransaction {
        val flow = DeleteAccountFlowInitiator(accountLinearId)
        return supplierNode.startFlow(flow).getOrThrow()
    }

    protected fun verifyNodeHasStates(
            node: StartedMockNode,
            num: Int,
            status: Vault.StateStatus) {
        val states = node.services.vaultService.queryBy<AccountState>(
                QueryCriteria.VaultQueryCriteria(status)).states
        assertEquals(node.info.toString(), num, states.size)
    }

    protected fun verifyNodeRecordedTxn(
            node: StartedMockNode,
            stx: SignedTransaction) {
        assertEquals(
                node.info.toString(),
                stx,
                node.services.validatedTransactions.getTransaction(stx.id))
    }

    protected val defaultCustomerDetails =
            CustomerDetails(
                    "John",
                    "Smith",
                    LocalDate.parse("1980-01-01"),
                    "1, London Wall")
}
