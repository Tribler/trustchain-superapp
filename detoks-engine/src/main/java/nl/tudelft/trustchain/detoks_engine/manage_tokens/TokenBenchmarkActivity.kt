package nl.tudelft.trustchain.detoks_engine.manage_tokens

import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.token_benchmark.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.detoksengine.sqldelight.Tokens
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks_engine.R
import nl.tudelft.trustchain.detoks_engine.trustchain.CommunityAdapter
import nl.tudelft.trustchain.detoks_engine.trustchain.TrustChainTransactionCommunity
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class TokenBenchmarkActivity : AppCompatActivity(R.layout.token_benchmark) {
    private lateinit var trustChainCommunity: TrustChainTransactionCommunity
    private lateinit var communityAdapter: CommunityAdapter

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var peerData: ArrayList<Peer>
    private lateinit var peerAdapter: ListAdapter<Peer>
    private val logger = KotlinLogging.logger {}

    private var selectedPeerIndex: Int = RecyclerView.NO_POSITION
    private var tokenCount: Int = 0
    private var packetsLost: Double = 100.00
    private var throughput: Long = 1000
    private var latency: Long = 0

    override fun onStart() {
        super.onStart()
        findViewById<Button>(R.id.send_1000).setOnClickListener{ _ -> send(1000)}
        findViewById<Button>(R.id.send_all).setOnClickListener{ _ -> send(tokenCount)}
        findViewById<Button>(R.id.send_1000_s).setOnClickListener{ _ -> sendPerSecond()}
        findViewById<Button>(R.id.gen_1000).setOnClickListener{ _ -> gen1000(1)}
        findViewById<Button>(R.id.gen_10000).setOnClickListener{ _ -> gen1000(10)}

        trustChainCommunity = IPv8Android.getInstance().getOverlay<TrustChainTransactionCommunity>()!!
        communityAdapter = CommunityAdapter.getInstance(trustChainCommunity)

        peerData = ArrayList(communityAdapter.getPeers())
        val peerListView = findViewById<RecyclerView>(R.id.peer_list)
        peerAdapter = ListAdapter(peerData, {peer -> peer.mid}, ::onPeerClick)
        peerListView.adapter = peerAdapter

        val tokenCounter = findViewById<TextView>(R.id.token_count)
        tokenCount = communityAdapter.tokenCount
        tokenCounter.text = tokenCount.toString()

//        val packetslostCounter = findViewById<TextView>(R.id.packetslost)
//        packetsLost = communityAdapter.packetsLost
//        packetslostCounter.text = "packets received: ${packetsLost.roundToInt()}%"
//
//        val throughputCounter = findViewById<TextView>(R.id.throughput)
//        throughput = communityAdapter.throughput
//        throughputCounter.text = "${(1000/throughput).toInt()} packets/second"

        val latencyCounter = findViewById<TextView>(R.id.latency)
        latency = communityAdapter.latency
        latencyCounter.text = "Latency: ${latency}ms"


        communityAdapter.setReceiveTransactionHandler {
            tokenCount = communityAdapter.tokenCount
            runOnUiThread {
                tokenCounter.text = tokenCount.toString()
            }
        }

        communityAdapter.setReceiveAgreementHandler {
            tokenCount = communityAdapter.tokenCount
            packetsLost = communityAdapter.packetsLost
            throughput = communityAdapter.throughput
            latency = communityAdapter.latency
            runOnUiThread {
                tokenCounter.text = tokenCount.toString()
//                packetslostCounter.text = "packets received: ${packetsLost.roundToInt()}%"
//                throughputCounter.text = "${(1000/throughput).toInt()} packets/second"
                latencyCounter.text = "Latency: ${latency}ms"
            }
        }
        val myId = trustChainCommunity.myPeer.mid.substring(0, 5)
        findViewById<TextView>(R.id.my_peer).text = "Peers (my id: ${myId}..)"

    }

    private fun sendPerSecond() {
        if (selectedPeerIndex == -1) {
            val toast = Toast.makeText(this, "Select a peer to send to", Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        scope.launch {
            repeat(tokenCount) {
                delay(1)
                communityAdapter.sendTokens(1, peerData[selectedPeerIndex])
            }
        }

    }

    private fun onPeerClick(index: Int) {
        selectedPeerIndex = index
    }
    private fun gen1000(times: Int) {
        repeat(times) {
            repeat(100) {
                val tokens = mutableListOf<String>()
                repeat(10) {
                    tokens.add(UUID.randomUUID().toString().substring(0, 8))
                }
                communityAdapter.injectTokens(tokens)
            }
        }
    }

    private fun send(nTokens: Int) {
        if (selectedPeerIndex == -1) {
            val toast = Toast.makeText(this, "Select a peer to send to", Toast.LENGTH_SHORT)
            toast.show()
            return
        }
        communityAdapter.sendTokens(nTokens, peerData[selectedPeerIndex])
    }

}
