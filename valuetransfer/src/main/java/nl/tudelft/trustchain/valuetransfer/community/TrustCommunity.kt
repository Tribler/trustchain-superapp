package nl.tudelft.trustchain.valuetransfer.community

import android.content.Context
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.db.TrustStore
import nl.tudelft.trustchain.valuetransfer.entity.TrustScore
import nl.tudelft.trustchain.valuetransfer.messaging.TrustPayload
import kotlin.collections.ArrayList
import kotlin.math.min

class TrustCommunity(
    private val store: TrustStore,
    private val context: Context
) : Community() {
    override val serviceId = "e77f0ba3a9e1e345295945cd13e1639fab5213ae"

    init {
        messageHandlers[TRUST_MESSAGE_ID] = ::onTrustMessage
    }

    /**
     * Receiver for trust data packages.
     * When a trust data package is received, this will process it to update the scores.
     * It wil first deserialize the scores, then for every score it will calculate a new weighted average.
     * These new values are then stored in the database.
     * @param packet: the data packet containing the serialized trust data.
     */
    private fun onTrustMessage(packet: Packet) {
        // Retrieve the signed payload
        val (peer, payload) = packet.getAuthPayload(TrustPayload.Deserializer)
        // Deserialize all the scores
        val scores = Json.decodeFromString<ArrayList<TrustScore>>(String(payload.scores))

        scope.launch {
            val store = store.trustDao()
            // Only parse data received from senders with a known trust score.
            val otherScore = store.getByKey(peer.publicKey)?.trustScore ?: return@launch
            // Now update the database values for each received score.
            scores.forEach {
                // Don't add any score about ourself
                if (it.publicKey != myPeer.publicKey) {
                    // Check if there is already a score for this specific key.
                    val currentScore = store.getByKey(it.publicKey)
                    // If there is no score, we create a new average based on no trust from ourself (0, 100%) and the received value.
                    if (currentScore == null) {
                        val storeValues = arrayListOf(Pair(it.trustScore, otherScore))
                        // No need to save our own input in the list of values
                        val calcValues = arrayListOf(Pair(0f, 100f))
                        calcValues.addAll(storeValues)
                        val score = getWeightedAverage(calcValues)
                        store.insert(TrustScore(it.publicKey, score, storeValues))
                    } else {
                        // If we already have a score, treat that score as 100% trusted (as its the score we gave it),
                        // and calculate the new average
                        val storeValues = arrayListOf(Pair(it.trustScore, otherScore))
                        if (currentScore.values != null) {
                            storeValues.addAll(currentScore.values)
                        }
                        // Don't store our own input in the database
                        val calcValues = arrayListOf(Pair(currentScore.trustScore, 100f))
                        calcValues.addAll(storeValues)
                        val score = getWeightedAverage(calcValues)
                        store.insert(TrustScore(it.publicKey, score, storeValues))
                    }
                }
            }
        }
    }

    /**
     * Responsible for sending 10 random trust scores from the database to another peer.
     * Will retrieve up to 10 scores and if there are any, send these.
     * The scores are serialized and wrapped in a [TrustPayload].
     * Preferably, the EVA protocol is used for sending the packet.
     * @param peer: the peer to which the package wil be send
     */
    suspend fun sendTrustScores(peer: Peer) {
        val scores: ArrayList<TrustScore> = ArrayList()
        // Get 10 random trust scores from our database
        scores.addAll(store.trustDao().getRandomN(10))

        // No need to send an empty package
        if (scores.isEmpty()) return

        // Encode the score data
        val payloadData = Json.encodeToString(scores)

        // Create the packet
        val payload = TrustPayload(payloadData.toByteArray())
        val packet = serializePacket(TRUST_MESSAGE_ID, payload, recipient = peer)

        // Send to the other party
        if (evaProtocolEnabled) {
            evaSendBinary(peer, EVA_TRUST_MESSAGE_ID, EVA_TRUST_MESSAGE_ID, packet)
        } else {
            send(peer, packet)
        }
    }

    /**
     * Generates a specified number of random trust scores.
     * For each score a public key is created as well as a random score.
     * @param number: the number of scores to generate
     * @return a list containing the n random scores created.
     */
    fun generateScores(number: Int): List<TrustScore> {
        val scores = mutableListOf<TrustScore>()
        for (i in 0 until number) {
            val key = defaultCryptoProvider.generateKey().pub()
            val score = (0..100).random().toFloat()
            scores.add(TrustScore(key, score, arrayListOf(Pair(score, 100f))))
        }
        return scores
    }

    /**
     * Based on a given list of scores, calculated the weighted average.
     * Each entry consists of a score given by a party and the trust score of the party itself.
     * The given score is then weighted by the score of the party itself.
     * All scores are bounded by 100(%).
     * @param values: the list of given scores and scores of the party that gave them
     * @return the calculated weighted average bounded by 100%
     */
    private fun getWeightedAverage(values: ArrayList<Pair<Float, Float>>): Float {
        var result = 0f
        var weight = 0f
        values.forEach { value ->
            val (score, senderScore) = value
            result += score * senderScore
            weight += senderScore
        }
        if (weight > 0) {
            result /= weight
        }
        // Cap at 100%
        return min(result, 100f)
    }

    companion object {
        const val EVA_TRUST_MESSAGE_ID = "eva_trust_package"
        const val TRUST_MESSAGE_ID = 1
    }

    class Factory(
        private val store: TrustStore,
        private val context: Context
    ) : Overlay.Factory<TrustCommunity>(TrustCommunity::class.java) {
        override fun create(): TrustCommunity {
            return TrustCommunity(store, context)
        }
    }
}
