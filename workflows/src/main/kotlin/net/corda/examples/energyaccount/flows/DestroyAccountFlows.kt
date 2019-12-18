package net.corda.examples.energyaccount.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.energyaccount.contracts.AccountContract

// *********
// * Flows *
// *********ac
@InitiatingFlow
@StartableByRPC
class DeleteAccountFlowInitiator(
        private val accountLinearId: UniqueIdentifier) : AccountBaseFlow() {

    companion object {
        object RETRIEVING : ProgressTracker.Step("Retrieving current account state.")
        object BUILDING : ProgressTracker.Step("Building and verifying transaction.")
        object SIGNING : ProgressTracker.Step("Signing transaction.")
        object COLLECTING : ProgressTracker.Step("Collecting counterparty signatures.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING : ProgressTracker.Step("Finalising transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }
    }

    override val progressTracker = ProgressTracker(
            RETRIEVING,
            BUILDING,
            SIGNING,
            COLLECTING,
            FINALISING)

    @Suspendable
    override fun call() : SignedTransaction {

        progressTracker.currentStep = RETRIEVING
        val account = getAccountByLinearId(accountLinearId)

        progressTracker.currentStep = BUILDING
        val utx = AccountContract.generateAccountDelete(notary, account)
        utx.verify(serviceHub)

        progressTracker.currentStep = SIGNING
        val pstx = serviceHub.signInitialTransaction(utx)

        progressTracker.currentStep = COLLECTING
        // Need the regulator to approve this action
        val regulatorSession = initiateFlow(account.state.data.regulator)
        val fstx = subFlow(CollectSignaturesFlow(
                pstx,
                listOf(regulatorSession),
                progressTracker = COLLECTING.childProgressTracker()))

        progressTracker.currentStep = FINALISING
        return subFlow(FinalityFlow(
                fstx,
                listOf(regulatorSession),
                FINALISING.childProgressTracker()))
    }
}

@InitiatedBy(DeleteAccountFlowInitiator::class)
class DeleteAccountFlowResponder(val counterpartySession: FlowSession) : AccountBaseFlow() {
    @Suspendable
    override fun call() : SignedTransaction {

        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                //  TODO: Add validation
            }
        }

        val txId = subFlow(signTransactionFlow).id

        // Regulator will implicitly record state consumption, since it will be aware of the
        // previous state of the account despite not being a participant in the delete txn
        return subFlow(ReceiveFinalityFlow(counterpartySession, txId))
    }
}
