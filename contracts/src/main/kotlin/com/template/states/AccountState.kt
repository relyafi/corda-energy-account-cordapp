package com.template.states

import com.template.contracts.AccountContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

// *********
// * State *
// *********
@BelongsToContract(AccountContract::class)
data class AccountState(val data: String, override val participants: List<AbstractParty> = listOf()) : ContractState
