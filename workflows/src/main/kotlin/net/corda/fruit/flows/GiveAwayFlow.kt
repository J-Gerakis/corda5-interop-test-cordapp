package net.corda.fruit.flows

import net.corda.fruit.contracts.FruitContract
import net.corda.fruit.states.FruitState
import net.corda.fruit.states.FruitState.FruitType
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
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory
import java.time.Instant

@InitiatingFlow
@StartableByRPC
class GiveAwayFlow @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) :
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
    lateinit var jsonMarshallingService: JsonMarshallingService

    @Suspendable
    override fun call(): SignedTransactionDigest {
        val mapOfParams = jsonMarshallingService.parseJson(params.parametersInJson, GiveAwayFlowJSON::class.java)

        val receiverList =  with(mapOfParams.receivers) { this.map{CordaX500Name.parse(it)} }

        val recipientPartyList = receiverList.map { name ->
            identityService.partyFromName(name)
                ?: throw NoSuchElementException("No party found for X500 name $name")
        }

        if(mapOfParams.quantity < recipientPartyList.size) { throw IllegalArgumentException("Insufficient quantity (${mapOfParams.quantity}) for the participants (${recipientPartyList.size})")}
        val fruit = FruitType.valueOf(mapOfParams.fruitType)
        val excess = mapOfParams.quantity % recipientPartyList.size
        val individualAmount = (mapOfParams.quantity - excess) / recipientPartyList.size

        val notary = notaryLookup.notaryIdentities.first()
        val us = flowIdentity.ourIdentity

        if(recipientPartyList.contains(us)) throw IllegalArgumentException("Emitter cannot be part of recipient's list")

        //1. Generate transaction
        val txCommand = Command(FruitContract.Commands.GiveAway(), recipientPartyList.map{ it.owningKey }.plus(us.owningKey) )
        val txBuilder = transactionBuilderFactory.create()
            .setNotary(notary)
            .addCommand(txCommand)
        recipientPartyList.forEach { participant ->
            txBuilder.addOutputState(FruitState(fruit,individualAmount,mapOfParams.message,participant, Instant.now()))
        }

        //2. Verify that the transaction is valid.
        txBuilder.verify()

        //3. Sign the transaction.
        val partSignedTx = txBuilder.sign()

        //4. Send the state to the counterparty, and receive it back with their signature.
        val otherPartySessions = recipientPartyList.map{ flowMessaging.initiateFlow(it) }
        val fullySignedTx = flowEngine.subFlow(CollectSignaturesFlow(partSignedTx, otherPartySessions))

        //5. Notarise and record the transaction in both parties' vaults.
        val notarisedTx = flowEngine.subFlow(
            FinalityFlow(
                fullySignedTx, otherPartySessions,
            )
        )

        return SignedTransactionDigest(
            notarisedTx.id,
            notarisedTx.tx.outputStates.map { output -> jsonMarshallingService.formatJson(output) },
            notarisedTx.sigs
        )
    }
}

@InitiatedBy(GiveAwayFlow::class)
class GiveAwayFlowAcceptor(val otherPartySession: FlowSession) : Flow<SignedTransaction> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    // instead, for now, doing this so it can be unit tested separately:
    fun isValid(stx: SignedTransaction) {
        requireThat {
            val outputs = stx.tx.outputs
            outputs.forEach { output ->
                "This must use fruit state objects." using (output.data is FruitState)
            }
        }
    }

    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
            override fun checkTransaction(stx: SignedTransaction) = isValid(stx)
        }
        val txId = flowEngine.subFlow(signTransactionFlow).id
        return flowEngine.subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
    }
}

data class GiveAwayFlowJSON(
    val receivers: List<String>,
    val fruitType: String,
    val quantity: Int,
    val message: String = ""
)