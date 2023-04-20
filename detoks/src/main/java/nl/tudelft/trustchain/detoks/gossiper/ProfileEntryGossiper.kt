package nl.tudelft.trustchain.detoks.gossiper

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.trustchain.detoks.DeToksCommunity
import nl.tudelft.trustchain.detoks.TorrentManager

class ProfileEntryGossiper(
    override val delay: Long,
    override val peers: Int,
    private val blocks: Int,
    private val context: Context
) : Gossiper() {

    override fun startGossip(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            while (coroutineScope.isActive) {
                gossip()
                delay(delay)
            }
        }
    }

    override suspend fun gossip() {
        val deToksCommunity = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!

        val randomPeers = pickRandomN(deToksCommunity.getPeers(), peers)
        val randomProfileEntries = pickRandomN(
            TorrentManager.getInstance(context).profile.profiles.entries.map { Pair(it.key, it.value) },
            blocks
        )
        if (randomPeers.isEmpty() || randomProfileEntries.isEmpty()) return

        randomProfileEntries.forEach { it1 ->
            val data = listOf(
                Pair("Key", it1.first),
                Pair("WatchTime", it1.second.watchTime.toString()),
                Pair("Likes", it1.second.likes.toString()),
                Pair("Duration", it1.second.duration.toString()),
                Pair("UploadDate", it1.second.uploadDate.toString())
            )
            randomPeers.forEach { it2 ->
                deToksCommunity.gossipWith(
                    it2,
                    ProfileEntryMessage(data),
                    DeToksCommunity.MESSAGE_PROFILE_ENTRY_ID
                )
            }
        }
    }
}

class ProfileEntryMessage(data: List<Pair<String, String>>) : GossipMessage<String>(data) {
    companion object Deserializer : Deserializable<ProfileEntryMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<ProfileEntryMessage, Int> {
            val msg = deserializeMessage(buffer, offset){
                return@deserializeMessage Pair(it.getString(0), it.getString(1))
            }
            return Pair(ProfileEntryMessage(msg.first), msg.second)
        }
    }
}
