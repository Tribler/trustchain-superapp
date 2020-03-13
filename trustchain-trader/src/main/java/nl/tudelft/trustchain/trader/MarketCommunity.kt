package nl.tudelft.trustchain.trader

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import nl.tudelft.ipv8.util.sha1
import nl.tudelft.ipv8.util.toHex
import java.util.*

class MarketCommunity: Community() {
    override val serviceId = sha1("4c69624e61434c504b3ab5bb7dc5a3a61de442585122b24c9f752469a212dc6d8ffa3d42bbf9c2f8d10ba569b270f615ef78aeff0547f38745d22af268037ad64935ee7c054b7921b23b".toByteArray()).toHex()
    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()

    val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()

    override fun walkTo(address: IPv4Address) {
        super.walkTo(address)

        discoveredAddressesContacted[address] = Date()
    }

    override fun onIntroductionResponse(peer: Peer, payload: IntroductionResponsePayload) {
        super.onIntroductionResponse(peer, payload)

        if (peer.address in DEFAULT_ADDRESSES) {
            lastTrackerResponses[peer.address] = Date()
        }
    }
}
