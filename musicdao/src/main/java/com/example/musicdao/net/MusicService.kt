package com.example.musicdao.net

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicdao.R
import com.example.musicdao.databinding.FragmentBlankBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.IPv8Configuration
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.OverlayConfiguration
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.BaseActivity
import java.util.*

class MusicService: BaseActivity() {
    private lateinit var binding: FragmentBlankBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var items: MutableList<String> = mutableListOf()

    private var peers: List<Peer> = listOf()
    override val navigationGraph = R.navigation.musicdao_navgraph
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_blank)
        supportActionBar?.title = "Music app"

        binding = FragmentBlankBinding.inflate(layoutInflater)

        viewManager = LinearLayoutManager(this)
        items.add("Listening for peers...")

        viewAdapter = MusicListAdapter(items)

        viewAdapter.notifyDataSetChanged()

        recyclerView = findViewById<RecyclerView>(R.id.recycler_view_items).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(false)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }

        val community = OverlayConfiguration(
            Overlay.Factory(MusicDemoCommunity::class.java),
            listOf(RandomWalk.Factory())
        )

        val config = IPv8Configuration(overlays = listOf(community))

        IPv8Android.Factory(application)
            .setConfiguration(config)
            .setPrivateKey(getPrivateKey())
            .init()

        items.add("I am " + IPv8Android.getInstance().myPeer.mid)
        viewAdapter.notifyDataSetChanged()

        val overlay = IPv8Android.getInstance().getOverlay<MusicDemoCommunity>()!!
        overlay.messageHandlers[MESSAGE_ID] = ::onMessage
        lifecycleScope.launch {
            while (isActive) {
                peers = overlay.getPeers()
                overlay.messageHandlers
                if (peers.isNotEmpty()) overlay.broadcastGreeting()
                delay(1000)
            }
        }
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(SongMessage.Deserializer)
        items.add("Song from" + peer.mid + ": " + payload.message + " at " + Date())
        viewAdapter.notifyDataSetChanged()
    }

    companion object {
        private const val PREF_PRIVATE_KEY = "private_key"
        private const val MESSAGE_ID = 1
    }

    private fun getPrivateKey(): PrivateKey {
        // Load a key from the shared preferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val privateKey = prefs.getString(PREF_PRIVATE_KEY, null)
        return if (privateKey == null) {
            // Generate a new key on the first launch
            val newKey = AndroidCryptoProvider.generateKey()
            prefs.edit()
                .putString(PREF_PRIVATE_KEY, newKey.keyToBin().toHex())
                .apply()
            newKey
        } else {
            AndroidCryptoProvider.keyFromPrivateBin(privateKey.hexToBytes())
        }
    }
}
