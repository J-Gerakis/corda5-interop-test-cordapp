package net.corda.fruit.schema

import net.corda.fruit.states.FruitState
import net.corda.v5.ledger.schemas.PersistentState
import net.corda.v5.persistence.MappedSchema
import net.corda.v5.persistence.UUIDConverter
import java.time.Instant
import java.util.*
import javax.persistence.*

object SalesOfferSchema

object SalesOfferSchemaV1 : MappedSchema(
    schemaFamily = SalesOfferSchema::class.java,
    version =1,
    mappedTypes = listOf(SalesOfferSchemaV1.PersistentSalesOffer::class.java)
) {
    override val migrationResource: String
        get() = "fruit.changelog-master"


    @Entity
    @NamedQueries(
        NamedQuery(
            name = "SalesOfferSchemaV1.PersistentSalesOffer.OpenOffers",
            query = "FROM net.corda.fruit.schema.SalesOfferSchemaV1\$PersistentSalesOffer "
        ),
        NamedQuery(
            name = "SalesOfferSchemaV1.PersistentSalesOffer.GetSalesOfferByDraftHash",
            query = "FROM net.corda.fruit.schema.SalesOfferSchemaV1\$PersistentSalesOffer salesState " +
                    "WHERE salesState.txIdHash = :hash"
        ),
        NamedQuery(
            name = "SalesOfferSchemaV1.PersistentSalesOffer.GetSalesOfferByFruitId",
            query = "FROM net.corda.fruit.schema.SalesOfferSchemaV1\$PersistentSalesOffer salesState " +
                    "WHERE salesState.stateId = :fruitId"
        )

    )
    @Table(name = "sales_offer_states")
    class PersistentSalesOffer(
        @Column(name = "draft_tx_id")
        var txIdHash: String,

        @Column(name = "state_id")
        @Convert(converter = UUIDConverter::class)
        var stateId: UUID,

        @Column(name = "price")
        var price:Double,

        @Column(name = "fruit_type")
        var fruit_type:String,

        @Column(name = "quantity")
        var quantity:Int,

        @Column(name = "seller_name")
        var sellerName:String,

        @Column(name = "buyer_name")
        var buyerName:String,

        @Column(name = "timestamp")
        var timestamp: Instant,

        @Column(name = "linear_id")
        @Convert(converter = UUIDConverter::class)
        var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this("",UUID.randomUUID(),0.0,FruitState.FruitType.APPLE.name,1,"","",Instant.now(),UUID.randomUUID())
    }

}