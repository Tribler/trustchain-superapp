package nl.tudelft.trustchain.atomicswap.swap

import nl.tudelft.trustchain.atomicswap.R
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.wallet.KeyChain
import kotlin.random.Random


/**
 * @param myAmount: The amount that you are giving away in the trade. Priced in [myCoin].
 * @param counterpartyAmount: The amount that you are receving. Priced in [toCoin].
 * @param myBitcoinTransaction: The bitcoin transaction the user has created
 *
 * Note: The amounts are in "full" coins, so if [myCoin] is BTC and [myAmount] is "0.01", that means
 * that "0.01" Bitcoin.
 */
data class Trade(
    val id: Long,
    var status: TradeOfferStatus,
    val myCoin: Currency,
    val myAmount: String,
    val counterpartyCoin: Currency,
    val counterpartyAmount: String,
) {

    var myPubKey: ByteArray? = null
        private set
    var myAddress: String? = null
        private set
    var counterpartyPubKey: ByteArray? = null
        private set
    var counterpartyAddress: String? = null
        private set
    var secret: ByteArray? = null
        private set
    var secretHash: ByteArray? = null
        private set
    var myBitcoinTransaction: ByteArray? = null
        private set
    var counterpartyBitcoinTransaction: ByteArray? = null
        private set

    // Called by the recipient
    fun setOnTrade(
        btcPubKey: ByteArray = WalletHolder.bitcoinWallet.freshKey(KeyChain.KeyPurpose.AUTHENTICATION).pubKey,
        ethAddress: String = WalletHolder.ethereumWallet.address()
    ) {
        myPubKey = btcPubKey
        myAddress = ethAddress
    }

    fun setOnInitiate(counterpartyPubKey: ByteArray, secretHash: ByteArray, counterpartyBitcoinTransaction: ByteArray?, counterpartyAddress: String?) {
        this.secretHash = secretHash
        this.counterpartyPubKey = counterpartyPubKey
        this.counterpartyBitcoinTransaction = counterpartyBitcoinTransaction
        this.counterpartyAddress = counterpartyAddress
    }

    fun setOnSecretObserved(secret: ByteArray){
        this.secret = secret
    }


    // Called by the initiator
    fun setOnAccept(
        counterpartyPubKey: ByteArray,
        ethAddress: String,
        btcPubKey: ByteArray = WalletHolder.bitcoinWallet.freshKey(KeyChain.KeyPurpose.AUTHENTICATION).pubKey,
        myEthAddress: String = WalletHolder.ethereumWallet.address()
    ) {
        myPubKey = btcPubKey
        myAddress = myEthAddress
        val randomSecret = Random.nextBytes(32)
        secret = randomSecret
        secretHash = Sha256Hash.hash(randomSecret)
        this.counterpartyPubKey = counterpartyPubKey
        counterpartyAddress = ethAddress
    }

    fun setOnComplete(counterpartyBitcoinTransaction: ByteArray){
        this.counterpartyBitcoinTransaction = counterpartyBitcoinTransaction
    }


    // Called by both
    fun setOnTransactionCreated(myBitcoinTransaction: ByteArray?){
        this.myBitcoinTransaction = myBitcoinTransaction
    }

}

enum class Currency(val currencyCodeStringResourceId: Int) {
    BTC(R.string.currency_code_bitcoin),
    ETH(R.string.currency_code_ethereum);

    companion object {
        fun fromString(coin: String): Currency {
            @Suppress("DEPRECATION")
            return when (coin.toLowerCase()) {
                "btc" -> BTC
                "eth" -> ETH
                else -> error("Currency not supported")
            }
        }
    }
}
