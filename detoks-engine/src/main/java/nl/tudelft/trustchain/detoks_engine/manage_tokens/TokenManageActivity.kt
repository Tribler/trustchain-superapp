package nl.tudelft.trustchain.detoks_engine.manage_tokens

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks_engine.R
import nl.tudelft.trustchain.detoks_engine.TransactionCommunity
import nl.tudelft.trustchain.detoks_engine.db.TokenStore
import java.util.UUID

class TokenManageActivity: AppCompatActivity(R.layout.token_manage) {
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
    }

    override fun onStart() {
        super.onStart()
        findViewById<Button>(R.id.refresh_button).setOnClickListener { _ -> refresh() }
        findViewById<Button>(R.id.send_button).setOnClickListener { _ -> send() }
        findViewById<Button>(R.id.generate_button).setOnClickListener { _ -> generate() }
        findViewById<Button>(R.id.delete_button).setOnClickListener{ _ -> delete()}


        tokenStore = TokenStore.getInstance(this)
        transactionCommunity = IPv8Android.getInstance().getOverlay()!!

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


        tokenAdapter = ListAdapter(tokenData, {t -> t}, ::onTokenClick)
        peerAdapter = ListAdapter(peerData, { peer -> peer.mid }, ::onPeerClick)
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
        tokenStore.storeToken(newToken)
        tokenData.add(newToken)
        tokenAdapter.notifyItemInserted(tokenData.size - 1)
    }

    fun delete() {
        val tokenIndex = selectedTokenIndex
        if (tokenIndex == -1) {
            val toast = Toast.makeText(this, "Select a token to delete", Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        val token = tokenData[tokenIndex]
        tokenStore.removeToken(token)
        selectedTokenIndex = RecyclerView.NO_POSITION
        tokenAdapter.removeAt(tokenIndex)
    }

    fun send() {
        val tokenIndex = selectedTokenIndex
        if (tokenIndex == -1) {
            val toast = Toast.makeText(this, "Select a token to send", Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        if (selectedPeerIndex == -1) {
            val toast = Toast.makeText(this, "Select a peer to send to", Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        val token = tokenData[tokenIndex]
        tokenStore.removeToken(token)
        transactionCommunity.send(peerData[selectedPeerIndex], token)
        selectedTokenIndex = RecyclerView.NO_POSITION
        tokenAdapter.removeAt(tokenIndex)
    }

}
