package net.corda.fruit.flows.special

import net.corda.client.rpc.flow.*
import net.corda.fruit.states.SalesOfferState
import net.corda.ledger.interop.impl.service.CNQCorda5Identity
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.httprpc.client.HttpRpcClient
import net.corda.v5.httprpc.client.config.HttpRpcClientConfig
import net.corda.v5.ledger.contracts.LinearState
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.vault.IdentityStateAndRefPostProcessor
import net.corda.v5.ledger.services.vault.StateStatus
import java.time.Duration
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

private const val DEFAULT_TIMEOUT = 20L
private val DEFAULT_QUERY_TIMEOUT: Duration = Duration.ofSeconds(DEFAULT_TIMEOUT)

fun Double.roundUp(numFractionDigits: Int = 2): Double {
    val factor = 10.0.pow(numFractionDigits.toDouble())
    return (this * factor).roundToInt() / factor
}

@Suspendable
@Suppress("UNUSED_PARAMETER")
fun <T : LinearState> PersistenceService.getLinearStateAndRefById(
    linearId: UUID,
    clazz: Class<T>
): StateAndRef<T>? {
    val params = mapOf(
            "uuid" to linearId,
            "stateStatus" to StateStatus.UNCONSUMED,
        )
    return customQuery(clazz, "LinearState.findByUuidAndStateStatus", params).singleOrNull()
}

@Suspendable
fun PersistenceService.checkFruitStateInvolvement(fruitId: UUID): StateAndRef<SalesOfferState>? {
    return customQuery(
        SalesOfferState::class.java,
        "SalesOfferSchemaV1.PersistentSalesOffer.GetSalesOfferByFruitId",
        mapOf("fruitId" to fruitId)
    ).singleOrNull()
}

@Suspendable
@Suppress("UNUSED_PARAMETER")
fun <T : LinearState> PersistenceService.customQuery(
    clazz: Class<T>,
    queryName: String,
    params: Map<String,Any>
): List<StateAndRef<T>> {
    return query<StateAndRef<T>>(
        queryName,
        params,
        IdentityStateAndRefPostProcessor.POST_PROCESSOR_NAME
    ).poll(1, DEFAULT_QUERY_TIMEOUT).values
}

@Suspendable
fun crossNetworkCall(
    cnqInfo: CNQCorda5Identity,
    flowOnOtherLedger: String,
    parametersInJson: String
): RpcFlowOutcomeResponse {
    val client = HttpRpcClient(
        baseAddress = cnqInfo.endpoint,
        rpcOpsClass = FlowStarterRPCOps::class.java,
        clientConfig = HttpRpcClientConfig().username(cnqInfo.username).password(cnqInfo.password)
            .minimumServerProtocolVersion(1),
        healthCheckInterval = 100
    )
    return client.use {
        val conn = client.start()
        val response: RpcStartFlowResponse = conn.proxy.startFlow(
            RpcStartFlowRequest(
                flowOnOtherLedger, "client-${UUID.randomUUID()}",
                RpcStartFlowRequestParameters(parametersInJson)
            )
        )
        var flowOutcome = conn.proxy.getFlowOutcome(response.flowId.uuid.toString())
        var count = 1
        while (flowOutcome.status == RpcFlowStatus.RUNNING && count < 100) {
            Thread.sleep(3000)
            flowOutcome = conn.proxy.getFlowOutcome(response.flowId.uuid.toString())
            count++
        }
        flowOutcome
    }
}