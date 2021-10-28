package net.corda.solarsystem.contracts

import net.corda.solarsystem.states.FruitState
import net.corda.v5.ledger.contracts.CommandData
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.contracts.requireSingleCommand
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.transactions.LedgerTransaction
import net.corda.v5.ledger.transactions.outputsOfType

class FruitContract : Contract {
    companion object {
        @JvmStatic
        val ID: String = FruitContract::class.java.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<Commands>()
        //if (command::class.java.name == "Exchange")

        requireThat {
            val out1 = tx.outputsOfType<FruitState>()[0]
            val out2 = tx.outputsOfType<FruitState>()[1]
            "Two output states should be created." using (tx.outputs.size == 2)
            "The emitter and receiver cannot be the same entity in the same state." using (out1.emitter != out1.receiver && out2.emitter != out2.receiver)
            "The emitter and receiver must match across states." using (out1.emitter == out2.receiver && out2.emitter == out1.receiver)

            "All of the participants must be signers." using (command.signers.containsAll(out1.participants.map { it.owningKey }))
            "All of the participants must be signers." using (command.signers.containsAll(out2.participants.map { it.owningKey }))
            "The quantity must at least 1" using (out1.quantity >= 1 && out2.quantity >= 1)
            "Tokens exchanged must be different" using (out1.fruitType != out2.fruitType)
        }
    }

    interface Commands : CommandData {
        class Exchange : Commands
        class GiveAway : Commands
    }
}