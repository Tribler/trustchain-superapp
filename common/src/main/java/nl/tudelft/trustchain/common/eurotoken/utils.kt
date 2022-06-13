package nl.tudelft.trustchain.common.eurotoken

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import java.math.BigInteger

fun verifyGatewayIdentity(publicKey: ByteArray, gatewayStore: GatewayStore): ValidationResult {
    if (gatewayStore.getGatewayFromPublicKey(publicKey) != null) {
        return ValidationResult.Valid
    }
    return ValidationResult.Valid
}

fun getBalanceChangeForBlock(block: TrustChainBlock?): Long {
    if (block == null) return 0
    if (block.transaction[TransactionRepository.KEY_AMOUNT]?.toString()?.contains("BTC") == true) return 0
    return if (block.isProposal) { // block is sending money
        -(block.transaction[TransactionRepository.KEY_AMOUNT]?.toString() ?: "0").toLong()
    } else { // block is receiving money
        (block.transaction[TransactionRepository.KEY_AMOUNT]?.toString() ?: "0").toLong()
    }
}

fun getVerifiedBalanceChangeForBlock(block: TrustChainBlock?): Long {
    if (block == null) return 0
    if (block.transaction[TransactionRepository.KEY_AMOUNT]?.toString()?.contains("BTC") == true) return 0
    if (block.isAgreement || block.type == TransactionRepository.BLOCK_TYPE_ROLLBACK) { // block is receiving money, don't add
        return 0
    } else { // block is sending money
        if (block.transaction[TransactionRepository.KEY_UNVERIFIED] == true) { // unverified, don't add
            return 0
        }
        return -((block.transaction[TransactionRepository.KEY_AMOUNT] ?: BigInteger.valueOf(0)) as BigInteger).toLong()
    }
}

fun getVerifiedBalanceForBlock(block: TrustChainBlock, database: TrustChainStore): Long? {
    if (block.isGenesis) return getVerifiedBalanceChangeForBlock(block)

    if (block.type == TransactionRepository.BLOCK_TYPE_CHECKPOINT && block.isProposal) {
        val linked = database.getLinked(block)
        if (linked != null) { // Found full checkpoint
            return (block.transaction[TransactionRepository.KEY_BALANCE] as Long)
        } else { // Found half checkpoint ignore and recurse
            val blockBefore = database.getBlockWithHash(block.previousHash) ?: return null
            return getVerifiedBalanceForBlock(blockBefore, database) // recurse
        }
    } else {
        val blockBefore = database.getBlockWithHash(block.previousHash) ?: return null
        val balance = getVerifiedBalanceForBlock(blockBefore, database) ?: return null
        return balance + getVerifiedBalanceChangeForBlock(block)
    }
}

fun getBalanceForBlock(block: TrustChainBlock, database: TrustChainStore): Long? {
    if (TransactionRepository.EUROTOKEN_TYPES.contains(block.type)) {
        if (block.isProposal && block.transaction[TransactionRepository.KEY_BALANCE] != null)
            return (block.transaction[TransactionRepository.KEY_BALANCE] as Long)
        if (block.isGenesis)
            return getBalanceChangeForBlock(block)
    }
    val blockBefore = database.getBlockWithHash(block.previousHash) ?: return null
    val balanceBefore = getBalanceForBlock(blockBefore, database) ?: return null
    return balanceBefore + getBalanceChangeForBlock(block)
}
