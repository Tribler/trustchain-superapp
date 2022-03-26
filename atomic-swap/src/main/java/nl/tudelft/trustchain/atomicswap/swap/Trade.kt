package nl.tudelft.trustchain.atomicswap.swap





/**
 * @param myAmount: The amount that you are giving away in the trade. Priced in [myCoin].
 * @param counterpartyAmount: The amount that you are receving. Priced in [toCoin].
 * @param myBitcoinTransaction: The bitcoin transaction the user has created
 *
 * Note: The amounts are in "full" coins, so if [myCoin] is BTC and [myAmount] is "0.01", that means
 * that "0.01" Bitcoin.
 */
data class Trade(

    // initator: on trade broadcast
    // recipient: on trade accept
    val id: Long,
    val myCoin : Currency,
    val myAmount : String,
    val counterpartyCoin : Currency,
    val counterpartyAmount: String,

    // Bob sends his pub key to Alice with the Accept message
    var myPubKey: ByteArray? = null,
    var myAddress: String? = null,

    var counterpartyPubKey: ByteArray? = null, //empty
    var counterpartyAddress: String? = null,

    var secret: ByteArray? = null, // only the creator
    var secretHash: ByteArray? = null,

    var myBitcoinTransaction: ByteArray? = null,
    var counterpartyBitcoinTransaction: ByteArray? = null,

    // Alice sends everything except the secret to Bob

    //1. btc to btc
    //2. eth to btc
    //3. btc to eth
)

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
