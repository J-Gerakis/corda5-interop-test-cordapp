package net.corda.solarsystem

import com.google.gson.GsonBuilder
import kong.unirest.HttpResponse
import kong.unirest.JsonNode
import kong.unirest.Unirest
import net.corda.test.dev.network.Node
import net.corda.test.dev.network.Nodes
import java.time.Duration
import java.util.*

fun Nodes<Node>.adele() = getNode("adele")
fun Nodes<Node>.bonnie() = getNode("bonnie")


fun retrieveOutcome(flowId: String): HttpResponse<JsonNode> {
    val request = Unirest.get("flowstarter/flowoutcome/$flowId").header("Content-Type", "application/json")
    return request.asJson()
}

fun createExchangeParams(receiverName:String, gives:String, givenQty:Int, wants:String, wantedQty:Int, message:String): String {
    return GsonBuilder()
        .create()
        .toJson(mapOf(
            "receiver" to receiverName,
            "gives" to gives,
            "given_quantity" to "$givenQty",
            "wants" to wants,
            "wanted_quantity" to "$wantedQty",
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


inline fun <R> eventually(
    duration: Duration = Duration.ofSeconds(5),
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