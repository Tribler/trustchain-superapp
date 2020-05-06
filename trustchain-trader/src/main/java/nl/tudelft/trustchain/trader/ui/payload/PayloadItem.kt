package nl.tudelft.trustchain.trader.ui.payload

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.common.constants.Currency
import nl.tudelft.trustchain.common.messaging.TradePayload

data class PayloadItem(
    val publicKey: ByteArray,
    val primaryCurrency: Currency,
    val secondaryCurrency: Currency,
    val availableAmount: Double?,
    val requiredAmount: Double?,
    val type: TradePayload.Type
) : Item() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PayloadItem

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (primaryCurrency != other.primaryCurrency) return false
        if (secondaryCurrency != other.secondaryCurrency) return false
        if (availableAmount != other.availableAmount) return false
        if (requiredAmount != other.requiredAmount) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + primaryCurrency.hashCode()
        result = 31 * result + secondaryCurrency.hashCode()
        result = 31 * result + (availableAmount?.hashCode() ?: 0)
        result = 31 * result + (requiredAmount?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        return result
    }
}
