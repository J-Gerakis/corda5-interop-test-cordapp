package net.corda.fruit.flows

import net.corda.fruit.contracts.FruitContract
import net.corda.fruit.states.FruitState
import net.corda.fruit.states.FruitType
import net.corda.systemflows.FinalityFlow
import net.corda.v5.application.flows.*
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory

@InitiatingFlow
@StartableByRPC
class IssueFruitFlow @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) : Flow<SignedTransactionDigest> {

    @CordaInject
    lateinit var flowEngine: FlowEngine
    @CordaInject
    lateinit var flowIdentity: FlowIdentity
    @CordaInject
    lateinit var transactionBuilderFactory: TransactionBuilderFactory
    @CordaInject
    lateinit var notaryLookup: NotaryLookupService
    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @Suspendable
    override fun call(): SignedTransactionDigest {
        val mapOfParams: Map<String, String> = jsonMarshallingService.parseJson(params.parametersInJson)

        val fruit = with(mapOfParams["fruit"] ?: throw BadRpcStartFlowRequestException("Parameter \"fruit\" missing.")) {
            FruitType.valueOf(this)
        }
        val qt1: Int = (mapOfParams["quantity"] ?: throw BadRpcStartFlowRequestException("Parameter \"quantity\" missing.")).toInt()


        val message = mapOfParams["message"] ?: ""

        val notary = notaryLookup.notaryIdentities.first()

        val us = flowIdentity.ourIdentity
        val fruitState1 = FruitState(fruit,qt1,message,us,us)

        val txCommand = Command(FruitContract.Commands.Issue(), fruitState1.participants.map { it.owningKey })

        val txBuilder = transactionBuilderFactory.create()
            .setNotary(notary)
            .addOutputState(fruitState1, FruitContract.ID,notary)
            .addCommand(txCommand)

        txBuilder.verify()

        val stx = txBuilder.sign()
        val notarisedTx = flowEngine.subFlow(FinalityFlow(stx, listOf()))
        return SignedTransactionDigest(
            notarisedTx.id,
            notarisedTx.tx.outputStates.map { output -> jsonMarshallingService.formatJson(output) },
            notarisedTx.sigs)

    }

}