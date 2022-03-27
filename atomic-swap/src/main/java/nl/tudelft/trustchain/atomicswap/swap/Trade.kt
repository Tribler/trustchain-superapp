package nl.tudelft.trustchain.atomicswap.swap

import nl.tudelft.ipv8.util.sha256
import org.bitcoinj.core.Address
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
    val myCoin : Currency,
    val myAmount : String,
    val counterpartyCoin : Currency,
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
    fun setOnTrade(){
        myPubKey = WalletHolder.bitcoinWallet.freshKey(KeyChain.KeyPurpose.AUTHENTICATION).pubKey
        myAddress= WalletHolder.ethereumWallet.address()
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
    fun setOnAccept(counterpartyPubKey: ByteArray, ethAddress: String) {
        myPubKey = WalletHolder.bitcoinWallet.freshKey(KeyChain.KeyPurpose.AUTHENTICATION).pubKey
        myAddress= WalletHolder.ethereumWallet.address()
        val randomSecret = Random.nextBytes(20)
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

enum class Currency{
    BTC(),
    ETH();

   companion object {
       fun fromString(coin: String): Currency {
           return when(coin.toLowerCase()){
               "btc" -> BTC
               "eth" -> ETH
               else -> error("Currency not supported")
           }
       }
   }
}
