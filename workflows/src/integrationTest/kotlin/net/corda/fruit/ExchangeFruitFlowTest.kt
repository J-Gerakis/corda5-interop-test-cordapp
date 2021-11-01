package net.corda.fruit

import kong.unirest.json.JSONObject
import net.corda.fruit.flows.ExchangeFaultyFlow
import net.corda.fruit.flows.ExchangeFruitFlow
import net.corda.fruit.states.FruitType
import net.corda.test.dev.network.*
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

class ExchangeFruitFlowTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            TestNetwork.forNetwork(NETWORK).verify {
                hasNode("adele").withFlow<ExchangeFruitFlow>()
                hasNode("bonnie").withFlow<ExchangeFruitFlow>()
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
                        val bodyContent = body.`object`.toMap()
                        Assertions.assertThat(bodyContent["status"]).isEqualTo("FAILED")
                        val exceptionContent = bodyContent["exceptionDigest"] as Map<*, *>
                        Assertions.assertThat(exceptionContent["message"].toString().startsWith("Contract verification failed: Failed requirement: All of the participants must be signers."))
                        Assertions.assertThat(exceptionContent["exceptionType"].toString().endsWith("ContractRejection"))
                    }
                }
            }

        }
    }

}

