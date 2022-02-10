package net.corda.fruit.states

import com.google.gson.Gson
import net.corda.fruit.contracts.FruitContract
import net.corda.fruit.schema.SalesOfferSchemaV1
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.utilities.JsonRepresentable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.BelongsToContract
import net.corda.v5.ledger.contracts.LinearState
import net.corda.v5.ledger.schemas.PersistentState
import net.corda.v5.ledger.schemas.QueryableState
import net.corda.v5.persistence.MappedSchema
import java.time.Instant

@BelongsToContract(FruitContract::class)
data class SalesOfferState(
    val exchangeDraftTxIdHash: String,
    val fruitStateId: UniqueIdentifier,
    val askingPrice: Double,
    val fruitType: FruitState.FruitType,
    val quantity: Int,
    val from: Party,
    val to: Party,
    val timestamp: Instant = Instant.now(),
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState, JsonRepresentable {

    override val participants: List<AbstractParty> get() = listOf(from,to)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is SalesOfferSchemaV1 -> SalesOfferSchemaV1.PersistentSalesOffer(
                this.exchangeDraftTxIdHash,
                this.fruitStateId.id,
                this.askingPrice,
                this.fruitType.name,
                this.quantity,
                this.from.name.toString(),
                this.to.name.toString(),
                this.timestamp,
                this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    private fun toDto(): SalesOfferDTO = SalesOfferDTO(
        exchangeDraftTxIdHash,
        fruitStateId.id.toString(),
        askingPrice,
        fruitType.name,
        quantity,
        from.name.toString(),
        timestamp.toString(),
        linearId.id.toString())

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(SalesOfferSchemaV1)
    override fun toJsonString(): String {
        return Gson().toJson(this.toDto())
    }

    data class SalesOfferDTO(
        val draftTxIdHash:String,
        val stateId:String,
        val askingPrice:Double,
        val fruitType:String,
        val quantity:Int,
        val from: String,
        val timestamp: String,
        val salesId:String,
    )
}