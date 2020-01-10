package net.corda.examples.energyaccount.clients.api

import net.corda.client.jackson.JacksonSupport
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.getOrThrow
import net.corda.examples.energyaccount.contracts.AccountState
import net.corda.examples.energyaccount.contracts.CustomerDetails
import net.corda.examples.energyaccount.flows.CreateAccountFlowInitiator
import net.corda.examples.energyaccount.flows.DeleteAccountFlowInitiator
import net.corda.examples.energyaccount.flows.TransferAccountFlowInitiator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger

@RestController
@RequestMapping("/api")
class AccountClientApi() {
    @Autowired
    lateinit var rpc: CordaRPCOps

    private val objectMapper = JacksonSupport.createNonRpcMapper()

    data class CreateTxnBody(val customerDetails: CustomerDetails)
    data class TransferTxnBody(val accountId: String, val toSupplier: String)
    data class DeleteTxnBody(val accountId: String)

    fun createAccount(customerDetails: CustomerDetails) : AccountState {

        // TODO: Remove hard coding of regulator
        val regulatorParty = rpc.wellKnownPartyFromX500Name(
                CordaX500Name("Government Regulator", "London", "GB")) ?:
                throw IllegalStateException("Could not resolve regulator identity")

        return rpc.startFlow(
                ::CreateAccountFlowInitiator,
                regulatorParty,
                customerDetails).returnValue.getOrThrow().coreTransaction.outputs.single().data
                as AccountState
    }

    fun deleteAccount(accountLinearId: UniqueIdentifier) {

       rpc.startFlow(::DeleteAccountFlowInitiator, accountLinearId).returnValue.getOrThrow()
    }

    fun getAccountByLinearId(id: UniqueIdentifier) : AccountState? {

        val results = rpc.vaultQueryByCriteria(
                QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id)),
                AccountState::class.java).states

        return ( if (results.size == 1) results.single().state.data else null )
    }

    @GetMapping(value = ["/getAccount"])
    fun getAccountByLinearId(@RequestParam id: String) : AccountState? {
        val uid = UniqueIdentifier.fromString(id)
        return getAccountByLinearId(uid)
    }

    @GetMapping(value = ["/getAllAccounts"])
    fun getAllAccounts() : List<AccountState> {
        val results = rpc.vaultQueryByCriteria(
                QueryCriteria.LinearStateQueryCriteria(),
                AccountState::class.java).states

        return results.map { it.state.data }
    }

    fun transferAccount(id: UniqueIdentifier, toSupplier: Party) : AccountState {

        return rpc.startFlow(
                ::TransferAccountFlowInitiator,
                id,
                toSupplier).returnValue.getOrThrow().coreTransaction.outputs.single().data
                as AccountState
    }

    @PostMapping(value = ["/createAccount"])
    fun createAccount(@RequestBody body: CreateTxnBody) : String {

        try {
            createAccount(body.customerDetails)
        } catch (e : FlowException) {
            return e.message!!
        }

        return "OK"
    }

    @PatchMapping(value = ["/transferAccount"])
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

    @DeleteMapping(value = ["/deleteAccount"])
    fun deleteAccount(@RequestBody body: DeleteTxnBody) : String {
        val uid = UniqueIdentifier.fromString(body.accountId)

        try {
            deleteAccount(uid)
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
