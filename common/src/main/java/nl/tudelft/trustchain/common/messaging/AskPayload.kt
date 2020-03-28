package nl.tudelft.trustchain.common.messaging

import android.util.Log
import nl.tudelft.ipv8.messaging.*
import nl.tudelft.trustchain.common.constants.Currency

class AskPayload(
    val askCurrency: Currency,      // Currency to be sold
    val paymentCurrency: Currency,  // Currency the seller want to be paid in
    val amount: Double,             // Amount of currency to be sold
    val price: Double               // Price per unit of currency
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(askCurrency.toString().toByteArray(Charsets.UTF_8)) +
            serializeVarLen(paymentCurrency.toString().toByteArray(Charsets.UTF_8)) +
            serializeVarLen(amount.toString().toByteArray(Charsets.UTF_8)) +
            serializeVarLen(price.toString().toByteArray(Charsets.UTF_8))
    }

    companion object Deserializer : Deserializable<AskPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<AskPayload, Int> {
            var localOffset = 0
            val (askCurrency, askCurrencySize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += askCurrencySize
            val (paymentCurrency, paymentCurrencySize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += paymentCurrencySize
            val (amount, amountSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += amountSize
            val (price, priceSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += priceSize
            val payload = AskPayload(
                Currency.valueOf(askCurrency.toString(Charsets.UTF_8)),
                Currency.valueOf(paymentCurrency.toString(Charsets.UTF_8)),
                amount.toString(Charsets.UTF_8).toDouble(),
                price.toString(Charsets.UTF_8).toDouble()
            )
            return Pair(payload, localOffset)
        }
    }
}
