package nl.tudelft.trustchain.liquidity.data

import androidx.core.content.ContentProviderCompat.requireContext
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment


class EuroTokenWallet(val transactionRepository: TransactionRepository) {


    fun getPublicKey(): PublicKey {
        return transactionRepository.trustChainCommunity.myPeer.publicKey
    }

    fun getWalletAddress(): String {
        return getPublicKey().toString()
    }

    fun getBalance(): Long {
        return transactionRepository.getMyVerifiedBalance()
    }


}
