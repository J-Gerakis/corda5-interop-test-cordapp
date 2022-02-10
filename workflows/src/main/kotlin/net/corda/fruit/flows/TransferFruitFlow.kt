package net.corda.fruit.flows

import net.corda.ledger.interop.api.CNQServices
import net.corda.systemflows.CollectSignaturesFlow
import net.corda.systemflows.FinalityFlow
import net.corda.v5.application.flows.*
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.services.TransactionService
import net.corda.v5.ledger.transactions.SignedTransactionDigest

@InitiatingFlow
@StartableByRPC
object TransferFruitFlow {
    class Initiator @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) :
        Flow<SignedTransactionDigest> {

        @CordaInject
        lateinit var flowEngine: FlowEngine
        @CordaInject
        lateinit var flowIdentity: FlowIdentity
        @CordaInject
        lateinit var flowMessaging: FlowMessaging
        @CordaInject
        lateinit var transactionService: TransactionService
        @CordaInject
        lateinit var identityService: IdentityService
        @CordaInject
        lateinit var cnqServices: CNQServices
        @CordaInject
        lateinit var notaryLookup: NotaryLookupService
        @CordaInject
        lateinit var persistenceService: PersistenceService
        @CordaInject
        lateinit var jsonMarshallingService: JsonMarshallingService

        ////WIP: Placeholder for later

        @Suppress("unused_parameter")
        @Suspendable
        @Throws(NumberFormatException::class, IllegalArgumentException::class)
        override fun call(): SignedTransactionDigest {
            val params = jsonMarshallingService.parseJson(params.parametersInJson, TransferJSON::class.java)
            val notary = notaryLookup.notaryIdentities.first()
            val us = flowIdentity.ourIdentity

            val draftTransaction = cnqServices.getUnverifiedTransaction(params.draftIdHash)
                ?: throw IllegalArgumentException("No transaction found for hash \"${params.draftIdHash}\"")

            //turn it into real transaction
            val stx = transactionService.sign(draftTransaction)
            val notarisedTx = flowEngine.subFlow(FinalityFlow(stx, listOf()))

            //TODO: counterPart and cross network flow

            return SignedTransactionDigest(
                notarisedTx.id,
                notarisedTx.tx.outputStates.map { output -> jsonMarshallingService.formatJson(output) },
                notarisedTx.sigs
            )
        }


        data class TransferJSON(
            val draftIdHash: String
        )
    }
}