package net.corda.examples.energyaccount.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.energyaccount.contracts.AccountContract

@InitiatingFlow
@StartableByRPC
class TransferAccountFlowInitiator(
        private val accountLinearId: UniqueIdentifier,
        private val newSupplier: Party) : AccountBaseFlow() {

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
        val utx = AccountContract.generateAccountTransfer(
                notary,
                account,
                newSupplier)
        utx.verify(serviceHub)

        progressTracker.currentStep = SIGNING
        val pstx = serviceHub.signInitialTransaction(utx)

        progressTracker.currentStep = COLLECTING
        // Need the regulator to approve this action
        val newSupplierSession = initiateFlow(newSupplier)
        val regulatorSession = initiateFlow(account.state.data.regulator)

        val sessions = listOf(newSupplierSession, regulatorSession)

        val fstx = subFlow(CollectSignaturesFlow(
                pstx,
                sessions,
                progressTracker = COLLECTING.childProgressTracker()))

        progressTracker.currentStep = FINALISING
        return subFlow(FinalityFlow(
                fstx,
                sessions,
                FINALISING.childProgressTracker()))
    }
}

@InitiatedBy(TransferAccountFlowInitiator::class)
class TransferAccountFlowResponder(val counterpartySession: FlowSession) : AccountBaseFlow() {
    @Suspendable
    override fun call() : SignedTransaction {

        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                //  TODO: Add validation
            }
        }

        val txId = subFlow(signTransactionFlow).id

        /* The regulator explicitly records the state despite not being a participant so they can
         * maintain an audit trail of all important actions, so we specify ALL_VISIBLE to achieve
         * this.*/
        return subFlow(ReceiveFinalityFlow(
                counterpartySession,
                txId,
                // TODO: Need to find a way of identifying that we're the regulator without \
                //       hard-coding an organisation name
                if (ourIdentity.name.organisation == "Government Regulator")
                    StatesToRecord.ALL_VISIBLE else StatesToRecord.ONLY_RELEVANT))
    }
}
