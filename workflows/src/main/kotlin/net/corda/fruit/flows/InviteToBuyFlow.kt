package net.corda.fruit.flows

import net.corda.fruit.contracts.FruitContract
import net.corda.fruit.flows.special.checkFruitStateInvolvement
import net.corda.fruit.flows.special.getLinearStateAndRefById
import net.corda.fruit.flows.special.roundUp
import net.corda.fruit.states.FruitState
import net.corda.fruit.states.SalesOfferState
import net.corda.ledger.interop.api.CNQServices
import net.corda.systemflows.CollectSignaturesFlow
import net.corda.systemflows.FinalityFlow
import net.corda.systemflows.ReceiveFinalityFlow
import net.corda.systemflows.SignTransactionFlow
import net.corda.v5.application.flows.*
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory

object InviteToBuyFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) :
        Flow<SignedTransactionDigest> {

        @CordaInject
        lateinit var flowEngine: FlowEngine
        @CordaInject
        lateinit var flowIdentity: FlowIdentity
        @CordaInject
        lateinit var flowMessaging: FlowMessaging
        @CordaInject
        lateinit var transactionBuilderFactory: TransactionBuilderFactory
        @CordaInject
        lateinit var identityService: IdentityService
        @CordaInject
        lateinit var notaryLookup: NotaryLookupService
        @CordaInject
        lateinit var persistenceService: PersistenceService
        @CordaInject
        lateinit var cnqServices: CNQServices
        @CordaInject
        lateinit var jsonMarshallingService: JsonMarshallingService

        @Suspendable
        @Throws(NumberFormatException::class, IllegalArgumentException::class)
        override fun call(): SignedTransactionDigest {
            val params = jsonMarshallingService.parseJson(params.parametersInJson, InviteToBuyJSON::class.java)
            val notary = notaryLookup.notaryIdentities.first()
            val us = flowIdentity.ourIdentity

            val recipientParty = identityService.partyFromName(CordaX500Name.parse(params.partyName))
                ?: throw NoSuchElementException("No party found for X500 name \"${params.partyName}\" ")

            val price = if (params.price < 0.0) throw IllegalArgumentException("Price cannot be negative")
            else params.price.roundUp()

            //check fruit state exists and get it
            val fruitId = UniqueIdentifier.fromString(params.stateId)
            val fruitStateAndRef = persistenceService.getLinearStateAndRefById(fruitId.id, FruitState::class.java)
                ?: throw IllegalArgumentException("No Fruit state found for id \"${fruitId.id}\"")

            //check fruit state is not already involved in another transaction
            val offerStateAndRef = persistenceService.checkFruitStateInvolvement(fruitId.id)
            if(offerStateAndRef != null) {
                throw IllegalArgumentException("Fruit state id \"${fruitId.id}\" already involved in Sale id \"${offerStateAndRef.state.data.linearId.id}\" ")
            }

            //Create transfer draft
            val draftCommand =
                Command(FruitContract.Commands.Transfer(), listOf(us.owningKey, recipientParty.owningKey))
            val draftBuilder = transactionBuilderFactory.create()
                .setNotary(notary)
                .addInputState(fruitStateAndRef)
                .addOutputState(fruitStateAndRef.state.data.withNewOwner(recipientParty), FruitContract.ID)
                .addCommand(draftCommand)
            draftBuilder.verify()

            val wireTx = draftBuilder.toWireTransaction()
            //Save into service (cache for the moment)
            cnqServices.addUnverifiedTransaction(wireTx, listOf(fruitId.id))

            //Create invite transaction
            val fruitState = fruitStateAndRef.state.data
            val salesOffer = SalesOfferState(wireTx.id.toString(), fruitId, price, fruitState.fruitType, fruitState.quantity, from = us, to = recipientParty)

            val inviteCommand =
                Command(FruitContract.Commands.InviteToBuy(), listOf(us.owningKey, recipientParty.owningKey))
            val inviteBuilder = transactionBuilderFactory.create()
                .setNotary(notary)
                .addOutputState(salesOffer, FruitContract.ID)
                .addCommand(inviteCommand)

            inviteBuilder.verify()
            val partSignedTx = inviteBuilder.sign()

            //Send the state to the counterparty, and receive it back with their signature.
            val otherPartySession = flowMessaging.initiateFlow(recipientParty)
            val fullySignedTx = flowEngine.subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession)))

            //Notarise and record the transaction in both parties' vaults.
            val notarisedTx = flowEngine.subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession)))

            return SignedTransactionDigest(
                notarisedTx.id,
                notarisedTx.tx.outputStates.map { output -> jsonMarshallingService.formatJson(output) },
                notarisedTx.sigs
            )
        }

        @InitiatedBy(InviteToBuyFlow.Initiator::class)
        class Acceptor(private val counterPartySession: FlowSession) : Flow<Unit> {

            @CordaInject
            lateinit var flowEngine: FlowEngine

            @Suspendable
            override fun call() {
                val signTransactionFlow = object : SignTransactionFlow(counterPartySession) {
                    override fun checkTransaction(stx: SignedTransaction) {
                        transactionMappingService.toLedgerTransaction(stx, false)
                    }
                }
                val txId = flowEngine.subFlow(signTransactionFlow).id
                flowEngine.subFlow(ReceiveFinalityFlow(counterPartySession, txId))
            }
        }

        data class InviteToBuyJSON(
            val partyName: String,
            val stateId: String,
            val price: Double
        )
    }
}