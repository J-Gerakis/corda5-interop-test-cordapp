package net.corda.solarsystem

import kong.unirest.json.JSONObject
import net.corda.solarsystem.flows.ExchangeFaultyFlow
import net.corda.solarsystem.flows.ExchangeFruitFlow
import net.corda.solarsystem.flows.LaunchProbeFlow
import net.corda.solarsystem.states.FruitType
import net.corda.test.dev.network.*
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

class ExchangeFruitFlowTest {

    companion object {
        const val NETWORK = "fruit-trading"

        @JvmStatic
        @BeforeAll
        fun setup() {
            TestNetwork.forNetwork(NETWORK).verify {
                hasNode("adele").withFlow<LaunchProbeFlow>()
                hasNode("bonnie").withFlow<LaunchProbeFlow>()
            }
        }
    }



    @Test
    fun `Happy path with two participants`() {
        TestNetwork.forNetwork(NETWORK).use {
            val receiver = bonnie()
            adele().httpRpc {
                val clientId = "client-${UUID.randomUUID()}"
                val flowId = with(startFlow(
                    flowName = ExchangeFruitFlow::class.java.name,
                    clientId = clientId,
                    parametersInJson = createExchangeParams(
                        receiverName = receiver.x500Name.toString(),
                        gives = FruitType.APPLE.name,
                        givenQty = 10,
                        wants = FruitType.BANANA.name,
                        wantedQty = 8,
                        message = "transaction 1"
                    )
                )){
                    Assertions.assertThat(status).isEqualTo(HttpStatus.SC_OK)
                    Assertions.assertThat(body.`object`.get("clientId")).isEqualTo(clientId)
                    val flowId = body.`object`.get("flowId") as JSONObject
                    Assertions.assertThat(flowId).isNotNull
                    flowId.get("uuid") as String
                }

                eventually {
                    with(retrieveOutcome(flowId)) {
                        Assertions.assertThat(status).isEqualTo(HttpStatus.SC_OK)
                        Assertions.assertThat(body.`object`.get("status")).isEqualTo("COMPLETED")
                    }
                }
            }

        }
    }

    @Test
    fun `Unhappy path with two participants`(){
        TestNetwork.forNetwork(NETWORK).use {
            val receiver = bonnie()
            adele().httpRpc {
                val clientId = "client-${UUID.randomUUID()}"
                val flowId = with(startFlow(
                    flowName = ExchangeFaultyFlow::class.java.name,
                    clientId = clientId,
                    parametersInJson = createExchangeParams(
                        receiverName = receiver.x500Name.toString(),
                        gives = FruitType.APPLE.name,
                        givenQty = 10,
                        wants = FruitType.BANANA.name,
                        wantedQty = 8,
                        message = "transaction 2 faulty"
                    )
                )){
                    Assertions.assertThat(status).isEqualTo(HttpStatus.SC_OK)
                    Assertions.assertThat(body.`object`.get("clientId")).isEqualTo(clientId)
                    val flowId = body.`object`.get("flowId") as JSONObject
                    Assertions.assertThat(flowId).isNotNull
                    flowId.get("uuid") as String
                }

                eventually {
                    with(retrieveOutcome(flowId)) {
                        Assertions.assertThat(status).isEqualTo(HttpStatus.SC_OK)
                        Assertions.assertThat(body.`object`.get("status")).isEqualTo("FAILED")
                    }
                }
            }

        }
    }

}

