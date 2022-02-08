package net.corda.fruit.schema

import net.corda.fruit.states.FruitState.FruitType
import net.corda.v5.ledger.schemas.PersistentState
import net.corda.v5.persistence.MappedSchema
import net.corda.v5.persistence.UUIDConverter
import java.time.Instant
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
    @NamedQueries(
        NamedQuery(
            name = "FruitSchemaV1.PersistentFruit.ListAll",
            query = "FROM net.corda.fruit.schema.FruitSchemaV1\$PersistentFruit"
        ),
        NamedQuery(
            name = "FruitSchemaV1.PersistentFruit.FindById",
            query = "FROM net.corda.fruit.schema.FruitSchemaV1\$PersistentFruit it " +
                    "WHERE it.linearId = :id"
        )
    )
    @Table(name = "fruit_states")
    class PersistentFruit(
        @Column(name = "fruit_type")
        var fruitType:String,

        @Column(name = "quantity")
        var quantity:Int,

        @Column(name = "message")
        var message:String,

        @Column(name = "owner_name")
        var ownerName:String,

        @Column(name = "timestamp")
        var timestamp:Instant,

        @Column(name = "linear_id")
        @Convert(converter = UUIDConverter::class)
        var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this(FruitType.APPLE.name, 1, "", "", Instant.now(), UUID.randomUUID())
    }
}