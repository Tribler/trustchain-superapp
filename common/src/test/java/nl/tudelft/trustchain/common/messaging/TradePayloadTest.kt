package nl.tudelft.trustchain.common.messaging

import nl.tudelft.trustchain.common.constants.Currency
import org.junit.Test
import org.junit.Assert.assertEquals

class TradePayloadTest {
    @Test
    fun serializeOutputDeserializesToOriginal() {
        val payload = TradePayload(
            byteArrayOf(0x00),
            Currency.DYMBE_DOLLAR,
            Currency.BTC,
            100.0,
            100.0,
            TradePayload.Type.ASK
        )
        val serializedPayload = payload.serialize()
        val deserializedPayload = TradePayload.deserialize(serializedPayload).first
        assertEquals(payload, deserializedPayload)
    }
}
