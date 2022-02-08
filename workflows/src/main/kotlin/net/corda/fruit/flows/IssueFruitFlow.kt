package net.corda.fruit.flows

import net.corda.fruit.contracts.FruitContract
import net.corda.fruit.states.FruitState
import net.corda.systemflows.FinalityFlow
import net.corda.v5.application.flows.*
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory
import java.lang.IllegalArgumentException

@InitiatingFlow
@StartableByRPC
class IssueFruitFlow @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) :
    Flow<SignedTransactionDigest> {

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
    @Throws(NumberFormatException::class, IllegalArgumentException::class)
    override fun call(): SignedTransactionDigest {
        val params = jsonMarshallingService.parseJson(params.parametersInJson, IssueFruitJSON::class.java)
        val notary = notaryLookup.notaryIdentities.first()
        val us = flowIdentity.ourIdentity

        val fruit = FruitState.FruitType.valueOf(params.fruitType)
        val qty = params.quantity
        if(qty <= 0) throw IllegalArgumentException("Inconsistent quantity")

        // Issuance
        val fruitState = FruitState(fruit,qty,params.message,us)

        val txCommand = Command(FruitContract.Commands.Issue(), listOf(us.owningKey) )
        val txBuilder = transactionBuilderFactory.create()
            .setNotary(notary)
            .addCommand(txCommand)
            .addOutputState(fruitState, FruitContract.ID)

        txBuilder.verify()

        val stx = txBuilder.sign()
        val notarisedTx = flowEngine.subFlow(FinalityFlow(stx, listOf()))
        return SignedTransactionDigest(
            notarisedTx.id,
            notarisedTx.tx.outputStates.map { output -> jsonMarshallingService.formatJson(output) },
            notarisedTx.sigs)
    }

}

data class IssueFruitJSON(
    val fruitType:String,
    val quantity: Int,
    val message: String = ""
)