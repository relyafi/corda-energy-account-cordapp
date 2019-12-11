package net.corda.examples.energyaccount.clients.api

import net.corda.client.jackson.JacksonSupport
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.examples.energyaccount.contracts.AccountState
import net.corda.examples.energyaccount.flows.CreateAccountFlowInitiator
import net.corda.examples.energyaccount.flows.DestroyAccountFlowInitiator
import net.corda.examples.energyaccount.flows.TransferAccountFlowInitiator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class AccountClientApi() {

    @Autowired
    lateinit var rpc: CordaRPCOps

    private val objectMapper = JacksonSupport.createNonRpcMapper()

    data class TransferTxnBody(val accountId: String, val toSupplier: String)

    fun createAccount(firstName: String, lastName: String) : AccountState {

        // TODO: Remove hard coding of regulator
        val regulatorParty = rpc.wellKnownPartyFromX500Name(
                CordaX500Name("Government Regulator", "London", "GB")) ?:
                throw IllegalStateException("Could not resolve regulator identity")

        return rpc.startFlow(
                ::CreateAccountFlowInitiator,
                regulatorParty,
                firstName,
                lastName).returnValue.getOrThrow().coreTransaction.outputs.single().data
                as AccountState
    }

    fun destroyAccount(accountLinearId: UniqueIdentifier) {

       rpc.startFlow(::DestroyAccountFlowInitiator, accountLinearId).returnValue.getOrThrow()
    }

    fun getAccountByLinearId(id: UniqueIdentifier) : AccountState? {

        val results = rpc.vaultQueryByCriteria(
                QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id)),
                AccountState::class.java).states

        return ( if (results.size == 1) results.single().state.data else null )
    }

    fun transferAccount(id: UniqueIdentifier, toSupplier: Party) : AccountState {

        return rpc.startFlow(
                ::TransferAccountFlowInitiator,
                id,
                toSupplier).returnValue.getOrThrow().coreTransaction.outputs.single().data
                as AccountState
    }

    @GetMapping(value = ["/getaccount"])
    fun getAccountByLinearId(@RequestParam id: String) : AccountState? {
        val uid = UniqueIdentifier.fromString(id)
        return getAccountByLinearId(uid)
    }

    @PatchMapping(value = ["/transferaccount"])
    fun transferAccount(@RequestBody body: TransferTxnBody) : String {
        val uid = UniqueIdentifier.fromString(body.accountId)
        val x500Name = CordaX500Name.parse(body.toSupplier)
        val party = rpc.wellKnownPartyFromX500Name(x500Name) ?:
                return "Unknown supplier $body.toSupplier"

        try {
            transferAccount(uid, party)
        } catch (e : FlowException) {
            return e.message!!
        }

        return "OK"
    }

    @GetMapping(value = ["/nodeInfo"], produces = arrayOf("text/plain"))
    fun nodeInfo(): String {
        return objectMapper.writeValueAsString(rpc.nodeInfo())
    }

    @GetMapping(value = ["/networkMap"], produces = arrayOf("text/plain"))
    fun networkMap(): String {
        return objectMapper.writeValueAsString(rpc.networkMapSnapshot())
    }
}
