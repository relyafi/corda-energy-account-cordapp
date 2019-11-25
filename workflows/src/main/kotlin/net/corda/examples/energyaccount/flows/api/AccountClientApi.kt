package net.corda.examples.energyaccount.flows.api

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.examples.energyaccount.contracts.AccountState
import net.corda.examples.energyaccount.flows.CreateAccountFlowInitiator
import net.corda.examples.energyaccount.flows.DestroyAccountFlowInitiator

class AccountClientApi(val rpc: CordaRPCOps) {

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
}
