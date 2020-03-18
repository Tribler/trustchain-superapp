package nl.tudelft.trustchain.trader.util

import android.util.Log
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.trustchain.trader.constants.BlockTypes

fun TrustChainBlock.getAmount(): Float {
    val amount = this.transaction["amount"].toString().toFloatOrNull()

    if (this.type != BlockTypes.DEMO_TX_BLOCK.value) {
        throw IllegalStateException("Block is of uncorrect type")
    }

    if (amount == null || amount < 0) {
        throw IllegalStateException("Block has invalid amount")
    } else {
        return amount
    }
}

fun TrustChainCommunity.getMyPublicKey(): ByteArray {
    return this.myPeer.publicKey.keyToBin()
}

@ExperimentalUnsignedTypes
fun TrustChainCommunity.getBalance(publicKey: ByteArray = this.getMyPublicKey()): Float {
    return this.database.getBalance(publicKey)
}

/**
 * Calculates the balance when looking at all block up to the specified sequence number. If no
 * sequence number is specified, it will calculate the current balance with all blocks taken into
 * account.
 */
@ExperimentalUnsignedTypes
fun TrustChainStore.getBalance(publicKey: ByteArray, sequenceNumber: UInt? = null): Float {
    val blockCount = this.getBlockCount().toInt()
    val txBlocks = this.getMutualBlocks(publicKey, limit = blockCount)
        .filter { sequenceNumber == null || it.sequenceNumber <= sequenceNumber }
        .filter { it.type == "demo_tx_block" }
        .filter { it.isAgreement || it.isSelfSigned }
        .reversed()

    var balance = 0F

    for (block in txBlocks) {
        val amount = block.getAmount()
        if (block.publicKey.contentEquals(publicKey)) {
            balance += amount
        } else if (block.linkPublicKey.contentEquals(publicKey)) {
            balance -= amount
        }
    }

    return balance
}
