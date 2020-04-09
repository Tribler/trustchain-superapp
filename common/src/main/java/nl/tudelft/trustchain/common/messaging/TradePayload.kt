package nl.tudelft.trustchain.common.messaging

import nl.tudelft.ipv8.messaging.*
import nl.tudelft.trustchain.common.constants.Currency

class TradePayload(
    val publicKey: ByteArray,
    val primaryCurrency: Currency,
    val secondaryCurrency: Currency,
    val amount: Double?,
    val price: Double?,
    val type: Type
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(publicKey) +
            serializeVarLen(primaryCurrency.toString().toByteArray(Charsets.UTF_8)) +
            serializeVarLen(secondaryCurrency.toString().toByteArray(Charsets.UTF_8)) +
            serializeVarLen(amount.toString().toByteArray(Charsets.UTF_8)) +
            serializeVarLen(price.toString().toByteArray(Charsets.UTF_8)) +
            serializeVarLen(type.toString().toByteArray(Charsets.UTF_8))
    }

    companion object Deserializer : Deserializable<TradePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TradePayload, Int> {
            var localOffset = 0
            val (publicKey, publicKeySize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += publicKeySize
            val (askCurrency, askCurrencySize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += askCurrencySize
            val (paymentCurrency, paymentCurrencySize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += paymentCurrencySize
            val (amount, amountSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += amountSize
            val (price, priceSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += priceSize
            val (type, typeSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += typeSize
            val payload = TradePayload(
                publicKey,
                Currency.valueOf(askCurrency.toString(Charsets.UTF_8)),
                Currency.valueOf(paymentCurrency.toString(Charsets.UTF_8)),
                amount.toString(Charsets.UTF_8).toDouble(),
                price.toString(Charsets.UTF_8).toDouble(),
                Type.valueOf(type.toString(Charsets.UTF_8))
            )
            return Pair(payload, localOffset)
        }
    }

    enum class Type {
        ASK,
        BID
    }
}
