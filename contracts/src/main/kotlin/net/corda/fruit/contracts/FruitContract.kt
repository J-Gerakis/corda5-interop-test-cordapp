package net.corda.fruit.contracts

import net.corda.fruit.states.FruitState
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
            else -> throw IllegalStateException("Unknown command: ${command.value}")
        }
    }

    private fun exchangeContractRules(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        requireThat {
            "Two output states should be created." using (tx.outputs.size == 2)
            val out1 = tx.outputsOfType<FruitState>()[0]
            val out2 = tx.outputsOfType<FruitState>()[1]

            "The emitter and receiver cannot be the same entity in the same state." using (out1.emitter != out1.receiver && out2.emitter != out2.receiver)
            "The emitter and receiver must match across states." using (out1.emitter == out2.receiver && out2.emitter == out1.receiver)

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
        class GiveAway : Commands, TypeOnlyCommandData()
    }
}