package net.corda.examples.energyaccount.flows

import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.examples.energyaccount.contracts.AccountState

abstract class AccountBaseFlow : FlowLogic<SignedTransaction>() {

    val notary get() = serviceHub.networkMapCache.notaryIdentities.firstOrNull()
            ?: throw FlowException("No available notary.")

    fun getAccountByLinearId(linearId: UniqueIdentifier): StateAndRef<AccountState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(linearId),
                Vault.StateStatus.UNCONSUMED,
                null)

        return serviceHub.vaultService
                .queryBy<AccountState>(queryCriteria).states.singleOrNull()
                ?: throw FlowException("Account with id $linearId not found.")
    }
}
