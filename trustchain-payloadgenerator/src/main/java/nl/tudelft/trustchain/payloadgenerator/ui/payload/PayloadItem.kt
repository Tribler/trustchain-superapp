package nl.tudelft.trustchain.payloadgenerator.ui.payload

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
) : Item()
