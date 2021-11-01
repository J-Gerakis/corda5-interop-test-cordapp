package net.corda.fruit.flows

import net.corda.fruit.contracts.FruitContract
import net.corda.fruit.states.FruitState
import net.corda.fruit.states.FruitType
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
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory

@InitiatingFlow
@StartableByRPC
class ExchangeFruitFlow @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) :
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
        val mapOfParams: Map<String, String> = jsonMarshallingService.parseJson(params.parametersInJson)

        val receiver = with(mapOfParams["receiver"] ?: throw BadRpcStartFlowRequestException("Parameter \"receiver\" missing.")) {
            CordaX500Name.parse(this)
        }
        val recipientParty = identityService.partyFromName(receiver)
            ?: throw NoSuchElementException("No party found for X500 name $receiver")

        val gives = with(mapOfParams["gives"] ?: throw BadRpcStartFlowRequestException("Parameter \"gives\" missing.")) {
            FruitType.valueOf(this)
        }
        val qt1 = with(mapOfParams["given_quantity"] ?: throw BadRpcStartFlowRequestException("Parameter \"given_quantity\" missing.")) {
            this.toInt()
        }
        val wants = with(mapOfParams["wants"] ?: throw BadRpcStartFlowRequestException("Parameter \"wants\" missing.")) {
            FruitType.valueOf(this)
        }
        val qt2 = with(mapOfParams["wanted_quantity"] ?: throw BadRpcStartFlowRequestException("Parameter \"wanted_quantity\" missing.")) {
            this.toInt()
        }
        val message = mapOfParams["message"] ?: ""

        val notary = notaryLookup.notaryIdentities.first()
        val us = flowIdentity.ourIdentity

        //1. Generate transaction
        val fruitState1 = FruitState(gives,qt1,message,us,recipientParty)
        val fruitState2 = FruitState(wants,qt2,message,recipientParty,us)
        val txCommand = Command(FruitContract.Commands.Exchange(), fruitState1.participants.map { it.owningKey })
        val txBuilder = transactionBuilderFactory.create()
            .setNotary(notary)
            .addOutputState(fruitState1, FruitContract.ID)
            .addOutputState(fruitState2, FruitContract.ID)
            .addCommand(txCommand)

        //2. Verify that the transaction is valid.
        txBuilder.verify()

        //3. Sign the transaction.
        val partSignedTx = txBuilder.sign()

        //4. Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = flowMessaging.initiateFlow(recipientParty)
        val fullySignedTx = flowEngine.subFlow(
            CollectSignaturesFlow(
                partSignedTx, setOf(otherPartySession),
            )
        )

        //5. Notarise and record the transaction in both parties' vaults.
        val notarisedTx = flowEngine.subFlow(
            FinalityFlow(
                fullySignedTx, setOf(otherPartySession),
            )
        )

        return SignedTransactionDigest(
            notarisedTx.id,
            notarisedTx.tx.outputStates.map { output -> jsonMarshallingService.formatJson(output) },
            notarisedTx.sigs
        )
    }
}


@InitiatedBy(ExchangeFruitFlow::class)
class ExchangeFruitFlowAcceptor(val otherPartySession: FlowSession) : Flow<SignedTransaction> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    // instead, for now, doing this so it can be unit tested separately:
    fun isValid(stx: SignedTransaction) {
        requireThat {
            val outputs = stx.tx.outputs
            "This must be an Exchange transaction." using (outputs[0].data is FruitState && outputs[1].data is FruitState)
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
