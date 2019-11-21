package net.corda.examples.energyaccount.flows

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before

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

    protected fun createAcount(
            supplierNode: StartedMockNode,
            firstName: String,
            lastName: String) : SignedTransaction {
        val flow = CreateAccountFlowInitiator(
                regulator.info.chooseIdentity(),
                firstName,
                lastName)
        return supplierNode.startFlow(flow).getOrThrow()
    }

    protected fun destroyAccount(
            supplierNode: StartedMockNode,
            accountLinearId: UniqueIdentifier) : SignedTransaction {
        val flow = DestroyAccountFlowInitiator(accountLinearId)
        return supplierNode.startFlow(flow).getOrThrow()
    }
}
