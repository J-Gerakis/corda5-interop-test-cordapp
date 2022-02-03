package net.corda.fruit.flows

import net.corda.fruit.states.FruitState
import net.corda.v5.application.flows.*
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.services.vault.IdentityStateAndRefPostProcessor
import java.time.Duration

@InitiatingFlow
@StartableByRPC
class GetFruitRecordFlow @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) :
    Flow<String> {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService
    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call(): String {

        val cursor = persistenceService.query<StateAndRef<FruitState>>(
            "FruitSchemaV1.PersistentFruit.FindAll",
            mapOf(),
            IdentityStateAndRefPostProcessor.POST_PROCESSOR_NAME
        )
        val answer = cursor.poll(100, Duration.ofSeconds(20L)).values

        val cursor2 = persistenceService.query<StateAndRef<FruitState>>(
            "FruitSchemaV1.PersistentFruit.FindAll",
            mapOf(),
            IdentityStateAndRefPostProcessor.POST_PROCESSOR_NAME
        )
        val answer2 = cursor2.poll(100, Duration.ofSeconds(20L)).values


        return jsonMarshallingService.formatJson(answer)
    }


}