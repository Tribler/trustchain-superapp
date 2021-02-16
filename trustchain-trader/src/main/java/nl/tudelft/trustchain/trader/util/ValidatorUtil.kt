package nl.tudelft.trustchain.trader.util

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.trustchain.trader.constants.BlockType

/**
 * Calculates the balance when looking at all block up to the specified sequence number. If no
 * sequence number is specified, it will calculate the current balance with all blocks taken into
 * account.
 */
@ExperimentalUnsignedTypes
fun getBalance(
    publicKey: ByteArray,
    database: TrustChainStore,
    sequenceNumber: UInt? = null
): Float {
    val blockCount = database.getBlockCount().toInt()
    val txBlocks = database.getMutualBlocks(publicKey, limit = blockCount)
        .filter { sequenceNumber == null || it.sequenceNumber <= sequenceNumber }
        .filter { it.type == "demo_tx_block" }
        .filter { it.isAgreement || it.isSelfSigned }
        .reversed()

    var balance = 0f

    for (block in txBlocks) {
        val amount = getAmount(block)
        if (block.publicKey.contentEquals(publicKey)) {
            balance += amount
        } else if (block.linkPublicKey.contentEquals(publicKey)) {
            balance -= amount
        }
    }

    return balance
}

/**
 * Returns the amount of currency from the block
 */
fun getAmount(block: TrustChainBlock): Float {
    val amount = block.transaction["amount"].toString().toFloatOrNull()

    if (block.type != BlockType.DEMO_TX_BLOCK.value) {
        throw IllegalStateException("Block is of incorrect type")
    }

    if (amount == null || amount < 0) {
        throw IllegalStateException("Block has invalid amount")
    } else {
        return amount
    }
}
