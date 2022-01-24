package net.corda.fruit

import kong.unirest.json.JSONObject
import net.corda.client.rpc.proxy.persistence.PersistenceRPCOps
import net.corda.client.rpc.proxy.persistence.RpcNamedQueryRequestBuilder
import net.corda.fruit.flows.ExchangeFaultyFlow
import net.corda.fruit.flows.ExchangeFruitFlow
import net.corda.fruit.flows.IssueFruitFlow
import net.corda.fruit.schema.FruitSchemaV1
import net.corda.fruit.states.FruitState
import net.corda.fruit.states.FruitType
import net.corda.test.dev.network.*
import net.corda.v5.base.util.seconds
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
                 with(
                    startFlow(
                        flowName = IssueFruitFlow::class.java.name,
                        clientId = clientId,
                        parametersInJson = createIssueParams(
                            fruit = FruitType.APPLE.name,
                            quantity = "10",
                            message = "transaction 1"
                        )
                    )
                ) {
                    Assertions.assertThat(status).isEqualTo(HttpStatus.SC_OK)
                    Assertions.assertThat(body.`object`.get("clientId")).isEqualTo(clientId)
                    val flowId = body.`object`.get("flowId") as JSONObject
                    Assertions.assertThat(flowId).isNotNull
                    flowId.get("uuid") as String
                }
            }

            val fruitInVault = getFruitInVault(adele())
            assert(fruitInVault.size >= 1)
            val apple : FruitSchemaV1.PersistentFruit  = fruitInVault[0]

            adele().httpRpc {
                val clientId = "client-${UUID.randomUUID()}"
                val flowId = with(startFlow(
                    flowName = ExchangeFruitFlow::class.java.name,
                    clientId = clientId,
                    parametersInJson = createExchangeParams(
                        receiverName = receiver.x500Name.toString(),
                        gives = apple.linearId.toString()
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

            val fruitInVaultB = getFruitInVault(bonnie())
            assert(fruitInVaultB.size >= 1)

        }
    }


    @Test
    fun `Issue Fruit`() {
        TestNetwork.forNetwork(NETWORK).use {
            adele().httpRpc {
                val clientId = "client-${UUID.randomUUID()}"
                with(
                    startFlow(
                        flowName = IssueFruitFlow::class.java.name,
                        clientId = clientId,
                        parametersInJson = createIssueParams(
                            fruit = FruitType.APPLE.name,
                            quantity = "10",
                            message = "transaction 1"
                        )
                    )
                ) {
                    Assertions.assertThat(status).isEqualTo(HttpStatus.SC_OK)
                    Assertions.assertThat(body.`object`.get("clientId")).isEqualTo(clientId)
                    val output = body.toString()
                    println(output)
                    val flowId = body.`object`.get("flowId") as JSONObject
                    Assertions.assertThat(flowId).isNotNull
                    flowId.get("uuid") as String
                }
            }

        }

    }




    @Test
    fun `Unhappy path with two participants`(){
        TestNetwork.forNetwork(NETWORK).use {
            val receiver = clarence()


            adele().httpRpc {
                val clientId = "client-${UUID.randomUUID()}"
                 with(
                    startFlow(
                        flowName = IssueFruitFlow::class.java.name,
                        clientId = clientId,
                        parametersInJson = createIssueParams(
                            fruit = FruitType.APPLE.name,
                            quantity = "10",
                            message = "transaction 1"
                        )
                    )
                ) {
                    Assertions.assertThat(status).isEqualTo(HttpStatus.SC_OK)
                    Assertions.assertThat(body.`object`.get("clientId")).isEqualTo(clientId)
                    val flowId = body.`object`.get("flowId") as JSONObject
                    Assertions.assertThat(flowId).isNotNull
                    flowId.get("uuid") as String
                }
            }

            val fruitInVault = getFruitInVault(adele())
            assert(fruitInVault.size >= 1)
            val apple : FruitSchemaV1.PersistentFruit  = fruitInVault[0]

            adele().httpRpc {
                val clientId = "client-${UUID.randomUUID()}"
                val flowId = with(startFlow(
                    flowName = ExchangeFaultyFlow::class.java.name,
                    clientId = clientId,
                    parametersInJson = createExchangeParams(
                        receiverName = receiver.x500Name.toString(),
                        gives = apple.linearId.toString(),
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

            val fruitInVaultC = getFruitInVault(clarence())
            assert(fruitInVaultC.size == 0) { "Clarence has ${fruitInVault.size} in her vault" }
        }
    }


}

