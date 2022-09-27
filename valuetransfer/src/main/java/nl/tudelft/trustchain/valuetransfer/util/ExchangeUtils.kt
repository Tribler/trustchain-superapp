package nl.tudelft.trustchain.valuetransfer.util

import nl.tudelft.ipv8.attestation.trustchain.ANY_COUNTERPARTY_PK // OK
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock // OK4
import nl.tudelft.ipv8.attestation.trustchain.UNKNOWN_SEQ // OK
import nl.tudelft.ipv8.keyvault.PublicKey // OK3
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.valuetransfer.ui.exchange.ExchangeTransactionItem

fun Transaction.toExchangeTransactionItem(
    myPk: PublicKey,
    blocks: List<TrustChainBlock>
): ExchangeTransactionItem {
    val isAnyCounterpartyPk = block.linkPublicKey.contentEquals(ANY_COUNTERPARTY_PK)
    val isMyPk = block.linkPublicKey.contentEquals(myPk.keyToBin())
    val isProposalBlock = block.linkSequenceNumber == UNKNOWN_SEQ

    // Some other (proposal) block is linked to the current agreement block. This is to find the status of incoming transactions.
    val hasLinkedBlock = blocks.find { it.linkedBlockId == block.blockId } != null

    // Some other (agreement) block is linked to the current proposal block. This is to find the status of outgoing transactions.
    val outgoingIsLinkedBlock = blocks.find { block.linkedBlockId == it.blockId } != null
    val status = when {
        hasLinkedBlock || outgoingIsLinkedBlock -> ExchangeTransactionItem.BlockStatus.SIGNED
        block.isSelfSigned -> ExchangeTransactionItem.BlockStatus.SELF_SIGNED
        isProposalBlock -> ExchangeTransactionItem.BlockStatus.WAITING_FOR_SIGNATURE
        else -> null
    }

    // Determine whether the transaction/block can be signed
    val canSign = (isAnyCounterpartyPk || isMyPk) &&
        isProposalBlock &&
        !block.isSelfSigned &&
        !hasLinkedBlock

    return ExchangeTransactionItem(
        this,
        canSign,
        status,
    )
}
