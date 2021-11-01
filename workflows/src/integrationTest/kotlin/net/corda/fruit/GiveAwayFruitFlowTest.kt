package net.corda.fruit

import kong.unirest.json.JSONObject
import net.corda.fruit.flows.GiveAwayFaultyFlow
import net.corda.fruit.flows.GiveAwayFlow
import net.corda.fruit.states.FruitType
import net.corda.test.dev.network.TestNetwork
import net.corda.test.dev.network.withFlow
import net.corda.test.dev.network.x500Name
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

class GiveAwayFruitFlowTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            TestNetwork.forNetwork(NETWORK).verify {
                hasNode("adele").withFlow<GiveAwayFlow>()
                hasNode("bonnie").withFlow<GiveAwayFlow>()
                hasNode("clarence").withFlow<GiveAwayFlow>()
                hasNode("danny").withFlow<GiveAwayFlow>()
            }
        }
    }



    @Test
    fun `Happy path with multiple participants`() {
        TestNetwork.forNetwork(NETWORK).use {
            val recipientList = listOf(
                bonnie().x500Name.toString(),
                clarence().x500Name.toString(),
                danny().x500Name.toString()
            )
            adele().httpRpc {
                val clientId = "client-${UUID.randomUUID()}"
                val flowId = with(startFlow(
                    flowName = GiveAwayFlow::class.java.name,
                    clientId = clientId,
                    parametersInJson = createGiveAwayParams(
                        recipients = recipientList,
                        type = FruitType.APPLE.name,
                        quantity = 30,
                        message = "transaction give away 1"
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
    fun `Unhappy path with multiple participants`() {
        TestNetwork.forNetwork(NETWORK).use {
            val recipientList = listOf(
                bonnie().x500Name.toString(),
                clarence().x500Name.toString(),
                danny().x500Name.toString()
            )
            adele().httpRpc {
                val clientId = "client-${UUID.randomUUID()}"
                val flowId = with(startFlow(
                    flowName = GiveAwayFaultyFlow::class.java.name,
                    clientId = clientId,
                    parametersInJson = createGiveAwayParams(
                        recipients = recipientList,
                        type = FruitType.APPLE.name,
                        quantity = 30,
                        message = "transaction give away 2 faulty"
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