package nl.tudelft.trustchain.common

import io.mockk.*
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.EndpointAggregator
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.trustchain.common.messaging.TradePayload
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketCommunityTest {
    private val peersSize = 5
    private var marketCommunity = spyk(MarketCommunity(), recordPrivateCalls = true)
    private val myPeer = mockk<Peer>()
    private val endpoint = mockk<EndpointAggregator>()

    @Test
    fun broadcast_callsSendOneTimePerPeer() {
        val payload = mockk<TradePayload>()
        every {
            marketCommunity["serializePacket"](
                any<Int>(),
                any<Serializable>(),
                any<Boolean>(),
                any<Peer>(),
                any<ByteArray>()
            )
        } returns byteArrayOf(0x00)
        every { marketCommunity.getPeers() } returns getFakePeers()
        every { marketCommunity.myPeer } returns myPeer
        every { marketCommunity.endpoint } returns endpoint
        every { endpoint.send(any<Peer>(), any()) } just runs

        marketCommunity.broadcast(payload)

        verify(exactly = peersSize) { endpoint.send(any<Peer>(), any()) }
    }

    @Test
    fun addListener_addsListener() {
        val beforeSize = marketCommunity.listenersMap[null]?.size ?: 0
        marketCommunity.addListener(null, mockk())
        val afterSize = marketCommunity.listenersMap[null]?.size ?: 0
        assertEquals(beforeSize, afterSize - 1)
    }

    private fun getFakePeers(): List<Peer> {
        val peers = mutableListOf<Peer>()
        for (i in 1..peersSize) {
            peers.add(mockk())
        }
        return peers
    }
}
