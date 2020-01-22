package net.corda.examples.energyaccount.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.energyaccount.contracts.AccountContract
import net.corda.examples.energyaccount.contracts.CustomerDetails

@InitiatingFlow
@StartableByRPC
// TODO The regulator party should be fixed and not specifiable on the transaction
class CreateAccountFlowInitiator(
        private val regulator: Party,
        private val customerDetails: CustomerDetails) : AccountBaseFlow() {

    companion object {
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
            BUILDING,
            SIGNING,
            COLLECTING,
            FINALISING)

    @Suspendable
    override fun call() : SignedTransaction {

        progressTracker.currentStep = BUILDING
        val utx = AccountContract.generateAccountCreate(
                notary,
                regulator,
                ourIdentity,
                customerDetails)
        utx.verify(serviceHub)

        progressTracker.currentStep = SIGNING
        val pstx = serviceHub.signInitialTransaction(utx)

        progressTracker.currentStep = COLLECTING
        // Need the regulator to approve this action
        val regulatorSession = initiateFlow(regulator)
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

@InitiatedBy(CreateAccountFlowInitiator::class)
class CreateAccountFlowResponder(val counterpartySession: FlowSession) : AccountBaseFlow() {
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
