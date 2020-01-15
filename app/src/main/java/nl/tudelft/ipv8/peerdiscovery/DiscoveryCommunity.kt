package nl.tudelft.ipv8.peerdiscovery

import android.util.Log
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.payload.BinMemberAuthenticationPayload
import nl.tudelft.ipv8.messaging.payload.GlobalTimeDistributionPayload
import nl.tudelft.ipv8.peerdiscovery.payload.PingPayload
import nl.tudelft.ipv8.peerdiscovery.payload.PongPayload
import nl.tudelft.ipv8.peerdiscovery.payload.SimilarityRequestPayload
import nl.tudelft.ipv8.peerdiscovery.payload.SimilarityResponsePayload

class DiscoveryCommunity(
    myPeer: Peer,
    endpoint: Endpoint,
    network: Network
) : Community(myPeer, endpoint, network) {
    override val serviceId = "7e313685c1912a141279f8248fc8db5899c5df5a"

    init {
        messageHandlers[MessageId.SIMILARITY_REQUEST] = ::handleSimilarityRequest
        messageHandlers[MessageId.SIMILARITY_RESPONSE] = ::handleSimilarityResponse
        messageHandlers[MessageId.PING] = ::handlePing
        messageHandlers[MessageId.PONG] = ::handlePong
    }



    internal fun handleSimilarityRequest(address: Address, bytes: ByteArray) {
        val (peer, remainder) = unwrapAuthPacket(address, bytes)
        val (dist, distSize) = GlobalTimeDistributionPayload.deserialize(remainder)
        val (payload, _) = SimilarityRequestPayload.deserialize(remainder, distSize)
        onSimilarityRequest(peer, dist, payload)
    }

    internal fun handleSimilarityResponse(address: Address, bytes: ByteArray) {
        val (peer, remainder) = unwrapAuthPacket(address, bytes)
        val (dist, distSize) = GlobalTimeDistributionPayload.deserialize(remainder)
        val (payload, _) = SimilarityResponsePayload.deserialize(remainder, distSize)
        onSimilarityResponse(peer, dist, payload)
    }

    internal fun handlePing(address: Address, bytes: ByteArray) {
        val (dist, distSize) = GlobalTimeDistributionPayload.deserialize(bytes)
        val (payload, _) = PingPayload.deserialize(bytes, distSize)
        onPing(address, payload)
    }

    internal fun handlePong(address: Address, bytes: ByteArray) {
        val (dist, distSize) = GlobalTimeDistributionPayload.deserialize(bytes)
        val (payload, _) = PongPayload.deserialize(bytes, distSize)
        onPong(address, payload)
    }

    internal fun onSimilarityRequest(
        peer: Peer,
        dist: GlobalTimeDistributionPayload,
        payload: SimilarityRequestPayload
    ) {
        Log.d("DiscoveryCommunity", "<- $payload")

        network.discoverServices(peer, payload.preferenceList)

        val myPeerSet = network.serviceOverlays.values.map { it.myPeer }.toSet()
        for (myPeer in myPeerSet) {
            val packet = createSimilarityResponse(payload.identifier, myPeer)
            endpoint.send(peer.address, packet)
        }
    }

    internal fun onSimilarityResponse(
        peer: Peer,
        dist: GlobalTimeDistributionPayload,
        payload: SimilarityResponsePayload
    ) {
        Log.d("DiscoveryCommunity", "<- $payload")

        if (maxPeers >= 0 && getPeers().size >= maxPeers && !network.verifiedPeers.contains(peer)) {
            Log.i("DiscoveryCommunity", "Dropping similarity response from $peer, too many peers!")
            return
        }

        network.addVerifiedPeer(peer)
        network.discoverServices(peer, payload.preferenceList)
    }

    internal fun onPing(
        address: Address,
        payload: PingPayload
    ) {
        Log.d("DiscoveryCommunity", "<- $payload")

        val packet = createPong(payload.identifier)
        endpoint.send(address, packet)
    }

    internal fun onPong(
        address: Address,
        payload: PongPayload
    ) {
        Log.d("DiscoveryCommunity", "<- $payload")

        // TODO
    }

    internal fun createSimilarityRequest() {
        // TODO
    }

    internal fun createSimilarityResponse(identifier: Int, peer: Peer): ByteArray {
        val globalTime = claimGlobalTime()
        val payload = SimilarityResponsePayload(identifier, getMyOverlays(peer))
        val auth = BinMemberAuthenticationPayload(peer.publicKey.keyToBin())
        val dist = GlobalTimeDistributionPayload(globalTime)
        Log.d("DiscoveryCommunity", "-> $payload")
        return serializePacket(prefix, MessageId.SIMILARITY_RESPONSE, listOf(auth, dist, payload))
    }

    internal fun createPong(identifier: Int): ByteArray {
        val globalTime = claimGlobalTime()
        val payload = PongPayload(identifier)
        val dist = GlobalTimeDistributionPayload(globalTime)
        Log.d("DiscoveryCommunity", "-> $payload")
        return serializePacket(prefix, MessageId.PONG, listOf(dist, payload), sign = false)
    }

    internal fun sendPing(peer: Peer) {
        val globalTime = claimGlobalTime()
        val payload = PingPayload((globalTime % 65536u).toInt())
        val dist = GlobalTimeDistributionPayload(globalTime)
        Log.d("DiscoveryCommunity", "-> $payload")
        val packet = serializePacket(prefix, MessageId.PING, listOf(dist, payload), sign = false)
        endpoint.send(peer.address, packet)
    }

    private fun getMyOverlays(peer: Peer): List<String> {
        return network.serviceOverlays
            .filter { it.value.myPeer == peer }
            .map { it.key }
    }

    object MessageId {
        const val SIMILARITY_REQUEST = 1
        const val SIMILARITY_RESPONSE = 2
        const val PING = 3
        const val PONG = 4
    }
}
