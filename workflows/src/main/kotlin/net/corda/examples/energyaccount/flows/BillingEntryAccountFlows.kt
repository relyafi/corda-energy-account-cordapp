package net.corda.examples.energyaccount.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.energyaccount.contracts.AccountContract
import net.corda.examples.energyaccount.contracts.BillingEntry
import java.math.BigDecimal
import java.time.LocalDateTime

@InitiatingFlow
@StartableByRPC
class BillingEntryAccountFlowInitiator(
        private val accountLinearId: UniqueIdentifier,
        private val entryType: EntryType,
        private val description: String?,
        private val amount: BigDecimal?,
        private val dateTime: LocalDateTime?) : AccountBaseFlow() {

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

        @CordaSerializable
        enum class EntryType { ADJUST, STANDARD }
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

        var fullDescription = ""
        var ccyAmount = BigDecimal(0.00)

        if (entryType == EntryType.ADJUST) {
            fullDescription = description!!
            ccyAmount = amount!!
        } else if (entryType == EntryType.STANDARD) {
            // Derive total amount from units consumed since last billing event
            // and ensure this information is appended to the description
            // TODO: Remove hard coding of per-unit cost - should be part of tariff info?
            val prcPerUnit =
                    if (ourIdentity.name.organisation == "British Energy")
                        0.15 else
                        0.16
            val unitsConsumed = getUnitsSinceLastBill(
                    account.state.data.billingEntries,
                    account.state.data.meterReadings)

            fullDescription = (description ?: "") + " ${unitsConsumed} units @ £${prcPerUnit}"
            ccyAmount = BigDecimal(unitsConsumed * prcPerUnit)
        }

        val utx = AccountContract.generateBillingEntry(
                notary,
                account,
                fullDescription,
                ccyAmount,
                dateTime)

        utx.verify(serviceHub)

        progressTracker.currentStep = SIGNING
        val pstx = serviceHub.signInitialTransaction(utx)

        progressTracker.currentStep = COLLECTING
        // Need the regulator to approve this action
        val regulatorSession = initiateFlow(account.state.data.regulator)

        val sessions = listOf(regulatorSession)

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

    private fun getUnitsSinceLastBill(billingEntries: List<BillingEntry>,
                                      meterReadings: List<Pair<LocalDateTime,Int>>) : Int {

        if (billingEntries.isNotEmpty() && meterReadings.isNotEmpty()) {

            val lastBillDT = billingEntries.last().eventDT

            if (meterReadings.last().first.isBefore(lastBillDT)) {
                return 0
            } else {
                var lastIdxAfterBill = meterReadings.size - 1

                while ( lastIdxAfterBill > 0 &&
                        meterReadings.elementAt(lastIdxAfterBill - 1)
                                .first.isAfter(lastBillDT)) {
                    --lastIdxAfterBill
                }

                if ( lastIdxAfterBill > 0 ) {
                    return meterReadings.last().second -
                            meterReadings.elementAt(lastIdxAfterBill - 1).second
                } else {
                    return meterReadings.last().second
                }
            }
        } else if (meterReadings.isNotEmpty()) {
            return meterReadings.last().second
        } else {
            return 0
        }
    }
}

@InitiatedBy(BillingEntryAccountFlowInitiator::class)
class BillingEntryAccountFlowResponder(val counterpartySession: FlowSession) : AccountBaseFlow() {
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
