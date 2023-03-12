package nl.tudelft.trustchain.detoks_engine.manage_tokens

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks_engine.R
import nl.tudelft.trustchain.detoks_engine.TransactionCommunity
import nl.tudelft.trustchain.detoks_engine.db.TokenStore
import java.util.UUID

class TokenManageActivity: AppCompatActivity() {
    private lateinit var tokenStore: TokenStore
    private lateinit var transactionCommunity: TransactionCommunity

    private lateinit var tokenData: ArrayList<String>
    private lateinit var peerData: ArrayList<Peer>
    private lateinit var tokenAdapter: ListAdapter<String>
    private lateinit var peerAdapter: ListAdapter<Peer>

    private var selectedTokenIndex: Int = RecyclerView.NO_POSITION
    private var selectedPeerIndex: Int = RecyclerView.NO_POSITION


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        tokenStore = TokenStore.getInstance(this)
        transactionCommunity = IPv8Android.getInstance().getOverlay()!!
    }

    override fun onStart() {
        super.onStart()
        findViewById<Button>(R.id.refresh_button).setOnClickListener { _ -> refresh() }
        findViewById<Button>(R.id.send_button).setOnClickListener { _ -> send() }
        findViewById<Button>(R.id.generate_button).setOnClickListener { _ -> generate() }




        tokenData = ArrayList(tokenStore.getAllToken())
        peerData = ArrayList(transactionCommunity.getPeers())
        val tokenListView = findViewById<RecyclerView>(R.id.token_list)
        val peerListView = findViewById<RecyclerView>(R.id.peer_list)

        transactionCommunity.setHandler {
                msg: String ->
            tokenStore.storeToken(msg)
            tokenData.add(msg)
            tokenAdapter.notifyItemInserted(tokenData.size - 1)
        }


        tokenAdapter = ListAdapter(ArrayList(tokenData), {t -> t}, ::onTokenClick)
        peerAdapter = ListAdapter(ArrayList(peerData), { peer -> peer.mid }, ::onPeerClick)
        tokenListView.adapter = tokenAdapter
        peerListView.adapter = peerAdapter
    }

    fun onTokenClick(index: Int) {
        selectedTokenIndex = index
    }

    fun onPeerClick(index: Int) {
        selectedPeerIndex = index
    }

    fun refresh() {
        tokenData = ArrayList(tokenStore.getAllToken())
        peerData = ArrayList(transactionCommunity.getPeers())
        tokenAdapter.notifyDataSetChanged()
        peerAdapter.notifyDataSetChanged()
    }

    fun generate() {
        val newToken: String = UUID.randomUUID().toString()
        tokenData.add(newToken)
        tokenAdapter.notifyItemInserted(tokenData.size - 1)
    }

    fun send() {
        val tokenIndex = selectedTokenIndex
        val token = tokenData[tokenIndex]
        tokenStore.removeToken(token)
        transactionCommunity.send(peerData[selectedPeerIndex], token)
        tokenAdapter.notifyItemRemoved(tokenIndex)
    }

}
