package net.corda.fruit.states

import com.google.gson.Gson
import net.corda.fruit.contracts.FruitContract
import net.corda.fruit.schema.FruitSchemaV1
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.utilities.JsonRepresentable
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.BelongsToContract
import net.corda.v5.ledger.contracts.CommandAndState
import net.corda.v5.ledger.contracts.LinearState
import net.corda.v5.ledger.contracts.OwnableState
import net.corda.v5.ledger.schemas.PersistentState
import net.corda.v5.ledger.schemas.QueryableState
import net.corda.v5.persistence.MappedSchema

@BelongsToContract(FruitContract::class)
data class FruitState(
    val fruitType:FruitType,
    val quantity:Int,
    val message:String = "",
    val emitter: Party,
    override val owner: AbstractParty,
    override val linearId: UniqueIdentifier = UniqueIdentifier()


) : LinearState, QueryableState, JsonRepresentable, OwnableState{
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(emitter, owner)

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        return CommandAndState(FruitContract.Commands.Exchange(), this.copy(owner = newOwner))
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is FruitSchemaV1 -> FruitSchemaV1.PersistentFruit(
                this.fruitType.name,
                this.quantity,
                this.message,
                this.emitter.name.toString(),
                this.owner.nameOrNull().toString(),
                this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    fun toDto(): FruitStateDto {
        return FruitStateDto(fruitType.name,quantity,message,
            emitter.name.toString(), owner.nameOrNull().toString(),
            linearId.id.toString())
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(FruitSchemaV1)
    override fun toJsonString(): String {
        return Gson().toJson(this.toDto())
    }

}

data class FruitStateDto(
    val fruitType:String,
    val quantity:Int,
    val message:String,
    val emitter:String,
    val owner:String,
    val linearId: String
)

@CordaSerializable
enum class FruitType{APPLE,BANANA}
