package nl.tudelft.trustchain.detoks_engine.manage_tokens

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import mu.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks_engine.R
import nl.tudelft.trustchain.detoks_engine.trustchain.CommunityAdapter
import nl.tudelft.trustchain.detoks_engine.trustchain.TrustChainTransactionCommunity
import java.util.UUID

class TokenManageActivity: AppCompatActivity(R.layout.token_manage) {
    private lateinit var trustChainCommunity: TrustChainTransactionCommunity
    private lateinit var communityAdapter: CommunityAdapter

    private lateinit var tokenData: ArrayList<String>
    private lateinit var peerData: ArrayList<Peer>
    private lateinit var tokenAdapter: ListAdapter<String>
    private lateinit var peerAdapter: ListAdapter<Peer>
    private val logger = KotlinLogging.logger {}

    private var selectedPeerIndex: Int = RecyclerView.NO_POSITION


    override fun onStart() {
        super.onStart()
        findViewById<Button>(R.id.refresh_button).setOnClickListener { _ -> refresh() }
        findViewById<Button>(R.id.send_button).setOnClickListener { _ -> send(1) }
        findViewById<Button>(R.id.generate_button).setOnClickListener { _ -> generate() }
        findViewById<Button>(R.id.send2_button).setOnClickListener{ _ -> send(2) }
        findViewById<Button>(R.id.send5_button).setOnClickListener{ _ -> send(5) }


        trustChainCommunity = IPv8Android.getInstance().getOverlay<TrustChainTransactionCommunity>()!!
        communityAdapter = CommunityAdapter(trustChainCommunity)


        tokenData = ArrayList(communityAdapter.getTokens())
        peerData = ArrayList(communityAdapter.getPeers())
        val tokenListView = findViewById<RecyclerView>(R.id.token_list)
        val peerListView = findViewById<RecyclerView>(R.id.peer_list)

        tokenAdapter = ListAdapter(tokenData, {t -> t}, {_ -> onTokenClick()})
        peerAdapter = ListAdapter(peerData, { peer -> peer.mid }, ::onPeerClick)
        tokenListView.adapter = tokenAdapter
        peerListView.adapter = peerAdapter



        communityAdapter.setReceiveTransactionHandler {
            transaction ->
            transaction.tokens.forEach{ t ->
                tokenData.add(t)
                runOnUiThread{ tokenAdapter.notifyItemInserted(tokenData.size - 1) }
            }
        }


        val myId = trustChainCommunity.myPeer.mid.substring(0, 5)
        findViewById<TextView>(R.id.my_peer).text = "Peers (my id: ${myId}..)"

    }

    fun onTokenClick() {
        val toast = Toast.makeText(this, "sending specific tokens is not supported", Toast.LENGTH_SHORT)
        toast.show()
    }

    fun onPeerClick(index: Int) {
        selectedPeerIndex = index
    }

    fun refresh() {
        tokenData.clear()
        tokenData.addAll(communityAdapter.getTokens())
        peerData.clear()
        peerData.addAll(communityAdapter.getPeers())
        tokenAdapter.notifyDataSetChanged()
        peerAdapter.notifyDataSetChanged()
    }

    fun generate() {
        val newToken: String = UUID.randomUUID().toString().substring(0, 4)
        communityAdapter.injectTokens(listOf(newToken))
    }

    fun send(nTokens: Int) {
        if (selectedPeerIndex == -1) {
            val toast = Toast.makeText(this, "Select a peer to send to", Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        repeat(nTokens) {
            tokenAdapter.removeAt(0)
        }
        communityAdapter.sendTokens(nTokens, peerData[selectedPeerIndex])
    }

}
