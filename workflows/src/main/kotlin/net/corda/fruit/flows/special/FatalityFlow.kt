package net.corda.fruit.flows.special

import net.corda.systemflows.*
import net.corda.systemflows.FinalityFlow
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.UnexpectedFlowEndException
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.MemberLookupService
import net.corda.v5.base.annotations.CordaInternal
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.debug
import net.corda.v5.crypto.SecureHash
import net.corda.v5.crypto.isFulfilledBy
import net.corda.v5.ledger.services.StatesToRecord
import net.corda.v5.ledger.services.TransactionMappingService
import net.corda.v5.ledger.services.TransactionService
import net.corda.v5.ledger.transactions.LedgerTransaction
import net.corda.v5.ledger.transactions.SignedTransaction
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class FatalityFlow constructor(
    private val transaction: SignedTransaction,
    private val sessions: Collection<FlowSession>,
    private val statesToRecord: StatesToRecord
) : Flow<SignedTransaction> {

    @CordaInternal
    data class ExtraConstructorArgs(val sessions: Collection<FlowSession>, val statesToRecord: StatesToRecord)

    @CordaInternal
    fun getExtraConstructorArgs() = ExtraConstructorArgs(sessions, statesToRecord)

    /**
     * Notarise the given transaction and broadcast it to the given [FlowSession]s. This list **must** at least include
     * all the non-local participants of the transaction. Sessions to non-participants can also be provided.
     *
     * @param transaction The signed transaction to be committed.
     */
    constructor(transaction: SignedTransaction, firstSession: FlowSession, vararg restSessions: FlowSession) : this(
        transaction, listOf(firstSession) + restSessions.asList()
    )

    /**
     * Notarise the given transaction and broadcast it to all the participants.
     *
     * @param transaction The signed transaction to be committed.
     * @param sessions A collection of [FlowSession]s for each non-local participant of the transaction. Sessions to non-participants can
     * also be provided.
     */
    constructor(transaction: SignedTransaction, sessions: Collection<FlowSession>) : this(transaction, sessions,
        StatesToRecord.ONLY_RELEVANT
    )

    private companion object {
        val logger = LoggerFactory.getLogger(FinalityFlow::class.java)
    }

    @CordaInject
    lateinit var flowEngine: FlowEngine
    @CordaInject
    lateinit var memberLookupService: MemberLookupService
    @CordaInject
    lateinit var transactionService: TransactionService
    @CordaInject
    lateinit var identityService: IdentityService
    @CordaInject
    lateinit var transactionMappingService: TransactionMappingService

    /**
     * @throws NotaryException if it fails to notarise [transaction].
     */
    @Suspendable
    override fun call(): SignedTransaction {

        require(sessions.none { it.counterparty.owningKey in memberLookupService.myInfo().identityKeys }) {
            "Do not provide flow sessions for the local node. FinalityFlow will record the notarised transaction locally."
        }

        // Note: this method is carefully broken up to minimize the amount of data reachable from the stack at
        // the point where subFlow is invoked, as that minimizes the checkpointing work to be done.
        //
        // Lookup the resolved transactions and use them to map each signed transaction to the list of participants.
        // Then send to the notary if needed, record locally and distribute.

        transaction.pushToLoggingContext()
        logCommandData()
//        val ledgerTransaction = verifyTx()

//        val externalTxParticipants = extractExternalParticipants(ledgerTransaction)
//        val sessionParties = sessions.map { it.counterparty }
//        val missingRecipients = externalTxParticipants - sessionParties
//        require(missingRecipients.isEmpty()) {
//            "Flow sessions were not provided for the following transaction participants: $missingRecipients"
//        }

        val notarised = notariseAndRecord()

        logger.debug { "Broadcasting transaction to $sessions" }

        for (session in sessions) {
            try {
                flowEngine.subFlow(SendTransactionFlow(session, notarised))
                logger.info("Party ${session.counterparty} received the transaction.")
            } catch (e: UnexpectedFlowEndException) {
                throw UnexpectedFlowEndException(
                    "${session.counterparty} has finished prematurely and we're trying to send them the finalised transaction. " +
                            "Did they forget to call ReceiveFinalityFlow? (${e.message})",
                    e.cause,
                    e.originalErrorId
                )
            }
        }
        logger.info("All parties received the transaction successfully.")

        return notarised
    }

    private fun logCommandData() {
        if (logger.isDebugEnabled) {
            val commandDataTypes = transaction.tx.commands.asSequence().mapNotNull { it.value::class.qualifiedName }.distinct()
            logger.debug("Started finalization, commands are ${commandDataTypes.joinToString(", ", "[", "]")}.")
        }
    }

    @Suspendable
    private fun notariseAndRecord(): SignedTransaction {
        val notarised = if (needsNotarySignature(transaction)) {
            logger.debug { "Notarising the transaction" }
            val notarySignatures = flowEngine.subFlow(NotaryClientFlow(transaction, skipVerification = true))
            transaction + notarySignatures
        } else {
            logger.info("No need to notarise this transaction.")
            transaction
        }
        logger.info("Recording transaction locally.")
        transactionService.record(statesToRecord, listOf(notarised))
        logger.info("Recorded transaction locally successfully.")
        return notarised
    }

    private fun needsNotarySignature(stx: SignedTransaction): Boolean {
        val wtx = stx.tx
        val needsNotarisation = wtx.inputs.isNotEmpty() || wtx.references.isNotEmpty() || wtx.timeWindow != null
        return needsNotarisation && hasNoNotarySignature(stx)
    }

    private fun hasNoNotarySignature(stx: SignedTransaction): Boolean {
        val notaryKey = stx.tx.notary?.owningKey
        val signers = stx.sigs.asSequence().map { it.by }.toSet()
        return notaryKey?.isFulfilledBy(signers) != true
    }

    private fun extractExternalParticipants(ltx: LedgerTransaction): Set<Party> {
        val participants = ltx.outputStates.flatMap { it.participants } + ltx.inputStates.flatMap { it.participants }
        return identityService.mapAndGroupAbstractPartyByParty(participants).keys - memberLookupService.myInfo().party
    }

    private fun verifyTx(): LedgerTransaction {
        val notary = transaction.tx.notary
        // The notary signature(s) are allowed to be missing but no others.
        if (notary != null) {
            transactionService.verifySignaturesExcept(transaction, notary.owningKey)
        } else {
            transactionService.verifyRequiredSignatures(transaction)
        }
        // TODO= [CORDA-3267] Remove duplicate signature verification
        return transactionMappingService.toLedgerTransaction(transaction, false).apply {
            verify()
        }
    }
}

/**
 * The receiving counterpart to [FinalityFlow].
 *
 * All parties who are receiving a finalised transaction from a sender flow must subcall this flow in their own flows.
 *
 * It's typical to have already signed the transaction proposal in the same workflow using [SignTransactionFlow]. If so
 * then the transaction ID can be passed in as an extra check to ensure the finalised transaction is the one that was signed
 * before it's committed to the vault.
 *
 * @param otherSideSession The session which is providing the transaction to record.
 * @param expectedTxId Expected ID of the transaction that's about to be received. This is typically retrieved from
 * [SignTransactionFlow]. Setting it to null disables the expected transaction ID check.
 * @param statesToRecord Which states to commit to the vault. Defaults to [StatesToRecord.ONLY_RELEVANT].
 */
//class ReceiveFatalityFlow @JvmOverloads constructor(
//    private val otherSideSession: FlowSession,
//    private val expectedTxId: SecureHash? = null,
//    private val statesToRecord: StatesToRecord = StatesToRecord.ONLY_RELEVANT
//) : Flow<SignedTransaction> {
//    @CordaInject
//    lateinit var flowEngine: FlowEngine
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        return flowEngine.subFlow(object : ReceiveTransactionFlow(otherSideSession, checkSufficientSignatures = true, statesToRecord = statesToRecord) {
//            override fun checkBeforeRecording(stx: SignedTransaction) {
//                require(expectedTxId == null || expectedTxId == stx.id) {
//                    "We expected to receive transaction with ID $expectedTxId but instead got ${stx.id}. Transaction was" +
//                            "not recorded and nor its states sent to the vault."
//                }
//            }
//        })
//    }
//}


//////////UTILS/////////////
fun SignedTransaction.pushToLoggingContext() {
    MDC.put("tx_id", id.toString())
}

fun IdentityService.mapAndGroupAbstractPartyByParty(parties: Collection<AbstractParty>): Map<Party, List<AbstractParty>> {
    val partyToPublicKey: Iterable<Pair<Party, AbstractParty>> = parties.map {
        (partyFromAnonymous(it)
            ?: throw IllegalArgumentException("Could not find Party for $it")) to it
    }
    return partyToPublicKey.toMultiMap()
}

fun <K, V> Iterable<Pair<K, V>>.toMultiMap(): Map<K, List<V>> = this.groupBy({ it.first }) { it.second }