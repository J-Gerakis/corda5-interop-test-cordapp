package net.corda.solarsystem.schema

import net.corda.solarsystem.states.FruitType
import net.corda.v5.ledger.schemas.PersistentState
import net.corda.v5.persistence.MappedSchema
import net.corda.v5.persistence.UUIDConverter
import java.util.*
import javax.persistence.*

object FruitSchema

object FruitSchemaV1: MappedSchema(
    schemaFamily = FruitSchema::class.java,
    version =1,
    mappedTypes = listOf(PersistentFruit::class.java)
) {

    override val migrationResource: String
        get() = "fruit.changelog-master"

    @Entity
    @NamedQuery(
        name = "FruitSchemaV1.PersistentFruit.FindAll",
        query = "FROM net.corda.solarsystem.schema.FruitSchemaV1\$PersistentFruit"
    )
    @Table(name = "fruit_states")
    class PersistentFruit(
        @Column(name = "fruit_type")
        var fruitType:String,

        @Column(name = "quantity")
        var quantity:Int,

        @Column(name = "message")
        var message:String,

        @Column(name = "emitter_name")
        var emitterName:String,

        @Column(name = "receiver_name")
        var receiverName:String,

        @Column(name = "linear_id")
        @Convert(converter = UUIDConverter::class)
        var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this(FruitType.APPLE.name, 1, "", "","", UUID.randomUUID())
    }
}