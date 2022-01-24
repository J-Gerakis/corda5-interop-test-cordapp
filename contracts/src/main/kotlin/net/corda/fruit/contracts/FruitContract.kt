package net.corda.fruit.contracts

import net.corda.fruit.states.FruitState
import net.corda.v5.ledger.contracts.*
import net.corda.v5.ledger.transactions.LedgerTransaction
import net.corda.v5.ledger.transactions.inputsOfType
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
            is Commands.Issue -> issueFruitRules(tx)
            else -> throw IllegalStateException("Unknown command: ${command.value}")
        }
    }

    private fun issueFruitRules(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        requireThat {
            "One State should be created." using (tx.outputs.size == 1)
            val out1 = tx.outputsOfType<FruitState>()[0]

            "All of the participants must be signers." using (command.signers.containsAll(out1.participants.map { it.owningKey }))
            "The quantity must at least 1" using (out1.quantity >= 1)
        }

    }

    private fun exchangeContractRules(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        requireThat {
            "One output states should be created." using (tx.outputs.size == 1)
            val out1 = tx.outputsOfType<FruitState>()[0]

            "One input state should be consumed." using  (tx.inputStates.size == 1)
            val input = tx.inputsOfType<FruitState>()[0]

            "The emitter and receiver cannot be the same entity in the same state." using (out1.emitter != out1.owner)

            "All of the participants must be signers." using (command.signers.containsAll(out1.participants.map { it.owningKey }))
            "All of the participants must be signers." using (command.signers.containsAll(input.participants.map { it.owningKey }))
            "The quantity must at least 1" using (out1.quantity >= 1 && input.quantity >= 1)
            "Tokens exchanged must be the same" using (out1.fruitType == input.fruitType)
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
        class Issue : Commands, TypeOnlyCommandData()
    }
}