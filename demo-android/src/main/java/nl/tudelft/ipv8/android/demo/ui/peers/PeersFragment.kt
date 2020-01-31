package nl.tudelft.ipv8.android.demo.ui.peers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_peers.*
import kotlinx.coroutines.*
import nl.tudelft.ipv8.android.demo.DemoCommunity
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.service.Ipv8Service
import nl.tudelft.ipv8.android.demo.ui.BaseFragment

class PeersFragment : BaseFragment() {
    private val adapter = ItemAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(PeerItemRenderer())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_peers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
    }

    override fun onServiceConnected(service: Ipv8Service) {
        loadNetworkInfo()
    }

    private fun loadNetworkInfo() {
        uiScope.launch {
            val service = service
            if (service != null) {
                val overlays = service.getOverlays()
                val demoCommunity = overlays.find { it is DemoCommunity }
                    ?: throw IllegalStateException("DemoCommunity is not configured")
                val peers = demoCommunity.getPeers()

                logger.debug("Found ${peers.size} community peers")
                val items = peers.map { PeerItem(it) }
                adapter.updateItems(items)
                txtCommunityName.text = demoCommunity.javaClass.simpleName
                txtPeerCount.text = resources.getQuantityString(
                    R.plurals.x_peers, peers.size,
                    peers.size)
                imgEmpty.isVisible = peers.isEmpty()
            }
            delay(1000)
            loadNetworkInfo()
        }
    }
}
