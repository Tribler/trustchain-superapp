package nl.tudelft.trustchain.detoks.gossiper

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.trustchain.detoks.DeToksCommunity
import nl.tudelft.trustchain.detoks.ProfileEntry
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

        randomPeers.forEach {
            deToksCommunity.gossipWith(
                it,
                ProfileEntryMessage(randomProfileEntries),
                DeToksCommunity.MESSAGE_PROFILE_ENTRY_ID
            )
        }
    }
}

class ProfileEntryMessage(data: List<Pair<String, ProfileEntry>>) : GossipMessage<ProfileEntry>(data) {
    companion object Deserializer : Deserializable<ProfileEntryMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<ProfileEntryMessage, Int> {
            val msg = deserializeMessage(buffer, offset){
                return@deserializeMessage Pair(it.getString(0), it.get(1) as ProfileEntry)
            }
            return Pair(ProfileEntryMessage(msg.first), msg.second)
        }
    }
}
