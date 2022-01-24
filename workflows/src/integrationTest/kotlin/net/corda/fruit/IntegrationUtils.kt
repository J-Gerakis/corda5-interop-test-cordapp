package net.corda.fruit

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kong.unirest.HttpResponse
import kong.unirest.JsonNode
import kong.unirest.Unirest
import net.corda.client.rpc.proxy.persistence.PersistenceRPCOps
import net.corda.client.rpc.proxy.persistence.RpcNamedQueryRequestBuilder
import net.corda.fruit.schema.FruitSchema
import net.corda.fruit.schema.FruitSchemaV1
import net.corda.fruit.states.FruitState
import net.corda.test.dev.network.Node
import net.corda.test.dev.network.Nodes
import net.corda.test.dev.network.httpRpcClient
import net.corda.v5.base.util.seconds
import java.time.Duration
import java.util.*

const val NETWORK = "fruit-trading"

fun Nodes<Node>.adele() = getNode("adele")
fun Nodes<Node>.bonnie() = getNode("bonnie")
fun Nodes<Node>.clarence() = getNode("clarence")
fun Nodes<Node>.danny() = getNode("danny")


fun retrieveOutcome(flowId: String): HttpResponse<JsonNode> {
    val request = Unirest.get("flowstarter/flowoutcome/$flowId").header("Content-Type", "application/json")
    return request.asJson()
}

fun createExchangeParams(receiverName:String, gives:String): String {
    return GsonBuilder()
        .create()
        .toJson(mapOf(
            "receiver" to receiverName,
            "gives" to gives
            )
        )
}

fun createIssueParams(fruit:String, quantity:String, message:String): String {
    return GsonBuilder()
        .create()
        .toJson(mapOf(
            "fruit" to fruit,
            "quantity" to quantity,
            "message" to message))
}

fun createGiveAwayParams(recipients: List<String>, type:String, quantity:Int, message:String): String {
    return GsonBuilder()
        .create()
        .toJson(mapOf(
            "receivers" to recipients,
            "fruitType" to type,
            "quantity" to "$quantity",
            "message" to message))
}

fun startFlow(
    flowName: String,
    clientId: String = "client-${UUID.randomUUID()}",
    parametersInJson: String
): HttpResponse<JsonNode> {
    val body = mapOf(
        "rpcStartFlowRequest" to
                mapOf(
                    "flowName" to flowName,
                    "clientId" to clientId,
                    "parameters" to mapOf("parametersInJson" to parametersInJson)
                )
    )
    val request = Unirest.post("flowstarter/startflow")
        .header("Content-Type", "application/json")
        .body(body)

    return request.asJson()
}

fun getFruitInVault(node: Node): MutableList<FruitSchemaV1.PersistentFruit> {
    val accumulatedFruitStates = mutableListOf<FruitSchemaV1.PersistentFruit>()
    node.httpRpcClient<PersistenceRPCOps, Unit> {
        val durableCursor = query(
            RpcNamedQueryRequestBuilder("FruitSchemaV1.PersistentFruit.FindAll")
                .build()
        )
            .build()

        var poll = durableCursor.poll(100, 20.seconds)
        poll.values.forEach {
            val fruitState = Gson().fromJson(it.json, FruitSchemaV1.PersistentFruit::class.java)
            accumulatedFruitStates.add(fruitState)
        }
    }
    return accumulatedFruitStates
}


inline fun <R> eventually(
    duration: Duration = Duration.ofSeconds(10),
    waitBetween: Duration = Duration.ofMillis(100),
    waitBefore: Duration = waitBetween,
    test: () -> R
): R {
    val end = System.nanoTime() + duration.toNanos()
    var times = 0
    var lastFailure: AssertionError? = null

    if (!waitBefore.isZero) Thread.sleep(waitBefore.toMillis())

    while (System.nanoTime() < end) {
        try {
            return test()
        } catch (e: AssertionError) {
            if (!waitBetween.isZero) Thread.sleep(waitBetween.toMillis())
            lastFailure = e
        }
        times++
    }

    throw AssertionError("Test failed with \"${lastFailure?.message}\" after $duration; attempted $times times")
}