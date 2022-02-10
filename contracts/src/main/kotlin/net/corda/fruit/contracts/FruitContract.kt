package net.corda.fruit.contracts

import net.corda.fruit.states.FruitState
import net.corda.fruit.states.SalesOfferState
import net.corda.v5.ledger.contracts.*
import net.corda.v5.ledger.transactions.LedgerTransaction
import net.corda.v5.ledger.transactions.outputsOfType
import java.lang.IllegalStateException
import java.security.PublicKey

class FruitContract : Contract {
    companion object {
        @JvmStatic
        val ID: String = FruitContract::class.java.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when(command.value){
            is Commands.Exchange -> exchangeContractRules(tx)
            is Commands.GiveAway -> giveAwayExchangeRules(tx)
            is Commands.Issue -> issueContractRules(tx)
            is Commands.InviteToBuy -> inviteToBuyRules(tx)
            is Commands.Transfer -> transferRules(tx)
            else -> throw IllegalStateException("Unknown command: ${command.value}")
        }
    }

    private fun issueContractRules(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        requireThat {
            "Unexpected inputs when issuing an artwork item" using tx.inputStates.isEmpty()
            "Issuing an artwork item requires a single output state" using (tx.outputStates.size == 1)
            "The output state must be of type ${FruitState::class.java.name}" using (tx.outputStates.single() is FruitState)
            val outputState = tx.outputStates.single() as FruitState
            "Only the owner needs to sign the transaction" using (command.signers.size == 1 && command.signers.single() == outputState.owner.owningKey)
        }
    }

    private fun inviteToBuyRules(tx: LedgerTransaction) {
        //val command = tx.commands.requireSingleCommand<Commands>()
        requireThat {
            "Issuing an offer to buy requires a single output state" using (tx.outputStates.size == 1)
            "The output state must be of type ${SalesOfferState::class.java.name}" using (tx.outputStates.single() is SalesOfferState)
        }
    }

    @Suppress("unused_parameter")
    private fun transferRules(tx: LedgerTransaction) {
        //TODO
    }

    //TODO: Exchange and GiveAway need to be modified or deleted, do not use them at the moment
    private fun exchangeContractRules(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        requireThat {
            //TODO: process input states
            "Two output states should be created." using (tx.outputs.size == 2)
            val out1 = tx.outputsOfType<FruitState>()[0]
            val out2 = tx.outputsOfType<FruitState>()[1]

            "The emitter and receiver cannot be the same entity in the same state." using (out1.owner != out2.owner)
            //"The emitter and receiver must match across states." using (out1.emitter == out2.receiver && out2.emitter == out1.receiver)

            "All of the participants must be signers." using (command.signers.containsAll(out1.participants.map { it.owningKey }))
            "All of the participants must be signers." using (command.signers.containsAll(out2.participants.map { it.owningKey }))
            "The quantity must at least 1" using (out1.quantity >= 1 && out2.quantity >= 1)
            "Tokens exchanged must be different" using (out1.fruitType != out2.fruitType)
        }
    }

    private fun giveAwayExchangeRules(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        val outs = tx.outputsOfType<FruitState>()
        val currentSigners = mutableSetOf<PublicKey>()
        outs.forEach { out -> currentSigners.addAll(out.participants.map { it.owningKey } ) }
        requireThat {
            "At least one output state should be created." using (tx.outputs.isNotEmpty())
            "All of the participants must be signers." using (command.signers.toSet() == currentSigners)
            //emitter is not a recipient -> checked in flow
        }
    }

    interface Commands : CommandData {
        class Exchange : Commands, TypeOnlyCommandData()
        class Issue : Commands, TypeOnlyCommandData()
        class Transfer : Commands, TypeOnlyCommandData()
        class InviteToBuy : Commands, TypeOnlyCommandData()
        class GiveAway : Commands, TypeOnlyCommandData()
    }
}