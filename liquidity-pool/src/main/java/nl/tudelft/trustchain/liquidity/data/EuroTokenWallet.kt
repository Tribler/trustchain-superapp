package nl.tudelft.trustchain.liquidity.data

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.ipv8.util.toHex

/**
 * Wallet class representing digitally stored euro tokens.
 */
class EuroTokenWallet(private val transactionRepository: TransactionRepository, private val publicKey: PublicKey) {
    /**
     * Gets the public key that can be used to perform transactions to this wallet.
     */
    fun getPublicKey(): PublicKey {
        return this.publicKey
    }

    /**
     * Gets the address used to address transactions to this wallet.
     */
    fun getWalletAddress(): String {
        return getPublicKey().keyToBin().toHex()
    }

    /**
     * Gets the current balance of this wallet.
     */
    fun getBalance(): Long {
        return transactionRepository.getMyVerifiedBalance()
    }

    fun getPoolOwners(): List<String> {
        return transactionRepository.getPoolOwners()
    }

    fun joinPool(recipient: ByteArray, btcHash: String, euroHash: String): TrustChainBlock? {
        return transactionRepository.sendJoinProposal(recipient, btcHash, euroHash)
    }

    fun sendTokens(recipient: ByteArray, amount: Long): TrustChainBlock? {
        return transactionRepository.sendTransferProposalSync(recipient, amount)
    }

    fun tradeTokens(recipient: ByteArray, hash: String, direction: String, receiveAddress: String): TrustChainBlock? {
        return transactionRepository.sendTradeProposal(recipient, hash, direction, receiveAddress)
    }
}
