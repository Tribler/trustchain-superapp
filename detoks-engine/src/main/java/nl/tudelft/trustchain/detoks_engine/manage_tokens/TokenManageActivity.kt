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
import nl.tudelft.trustchain.detoks_engine.db.TokenStore
import nl.tudelft.trustchain.detoks_engine.trustchain.CommunityAdapter
import nl.tudelft.trustchain.detoks_engine.trustchain.TrustChainTransactionCommunity
import java.util.UUID

class TokenManageActivity: AppCompatActivity(R.layout.token_manage) {
    private lateinit var tokenStore: TokenStore
    private lateinit var trustChainCommunity: TrustChainTransactionCommunity
    private lateinit var communityAdapter: CommunityAdapter

    private lateinit var tokenData: ArrayList<String>
    private lateinit var peerData: ArrayList<Peer>
    private lateinit var tokenAdapter: ListAdapter<String>
    private lateinit var peerAdapter: ListAdapter<Peer>
    private val logger = KotlinLogging.logger {}

    private val transactionsInTransit: MutableMap<String, Transaction> = mutableMapOf()

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
        findViewById<Button>(R.id.delete_button).setOnClickListener{ _ -> delete() }
        findViewById<Button>(R.id.delete_all_button).setOnClickListener{ _ -> deleteAll() }


        tokenStore = TokenStore.getInstance(this)
        trustChainCommunity = IPv8Android.getInstance().getOverlay<TrustChainTransactionCommunity>()!!
        communityAdapter = CommunityAdapter(trustChainCommunity)

        tokenData = ArrayList(tokenStore.getAllToken())
        peerData = ArrayList(communityAdapter.getPeers())
        val tokenListView = findViewById<RecyclerView>(R.id.token_list)
        val peerListView = findViewById<RecyclerView>(R.id.peer_list)

        tokenAdapter = ListAdapter(tokenData, {t -> t}, ::onTokenClick)
        peerAdapter = ListAdapter(peerData, { peer -> peer.mid }, ::onPeerClick)
        tokenListView.adapter = tokenAdapter
        peerListView.adapter = peerAdapter


        communityAdapter.setReceiveAckHandler {
            transactionId ->
            val succeededTransaction = transactionsInTransit[transactionId]
            transactionsInTransit.remove(transactionId)
            succeededTransaction?.tokens?.forEach { t ->
                tokenStore.removeToken(t)
                runOnUiThread{tokenAdapter.removeAt(tokenData.indexOf("... $t"))}
            }
        }

        communityAdapter.setReceiveTransactionHandler {
            transaction ->
            transaction.tokens.forEach{ t ->
                tokenStore.storeToken(t)
                tokenData.add(t)
                runOnUiThread{ tokenAdapter.notifyItemInserted(tokenData.size - 1) }
            }
        }

//        transactionCommunity.setHandler {
//                msg: String ->
//            tokenStore.storeToken(msg)
//            tokenData.add(msg)
//            runOnUiThread{ tokenAdapter.notifyItemInserted(tokenData.size - 1) }
//        }

        val myId = trustChainCommunity.myPeer.mid.substring(0, 5)
        findViewById<TextView>(R.id.my_peer).text = "Peers (my id: ${myId}..)"

    }

    fun onTokenClick(index: Int) {
        selectedTokenIndex = index
    }

    fun onPeerClick(index: Int) {
        selectedPeerIndex = index
    }

    fun refresh() {
        tokenData.clear()
        tokenData.addAll(tokenStore.getAllToken())
        peerData.clear()
        peerData.addAll(communityAdapter.getPeers())
        tokenAdapter.notifyDataSetChanged()
        peerAdapter.notifyDataSetChanged()
    }

    fun generate() {
        val newToken: String = UUID.randomUUID().toString().substring(0, 4)
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

    fun deleteAll() {
        for (token in tokenData) {
            tokenStore.removeToken(token)
        }
        tokenAdapter.removeAll()
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
        tokenData[tokenIndex] = "... $token"
        tokenAdapter.notifyItemChanged(tokenIndex)
        val transaction = Transaction(listOf(token))
        transactionsInTransit[transaction.transactionId] = transaction
        communityAdapter.proposeTransaction(transaction, peerData[selectedPeerIndex])
        selectedTokenIndex = RecyclerView.NO_POSITION
    }

}
