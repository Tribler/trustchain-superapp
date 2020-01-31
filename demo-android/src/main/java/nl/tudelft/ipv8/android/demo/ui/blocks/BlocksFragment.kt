package nl.tudelft.ipv8.android.demo.ui.blocks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_blocks.*
import kotlinx.android.synthetic.main.fragment_peers.recyclerView
import nl.tudelft.ipv8.android.demo.DemoCommunity
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.service.Ipv8Service
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import nl.tudelft.ipv8.util.hexToBytes

class BlocksFragment : BaseFragment() {
    private val adapter = ItemAdapter()

    private lateinit var publicKey: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = BlocksFragmentArgs.fromBundle(arguments!!)
        publicKey = args.publicKey.hexToBytes()

        adapter.registerRenderer(BlockItemRenderer())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_blocks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
    }

    override fun onServiceConnected(service: Ipv8Service) {
        val overlays = service.getOverlays()
        val demoCommunity = overlays.find { it is DemoCommunity } as? DemoCommunity
            ?: throw IllegalStateException("DemoCommunity is not configured")
        val blocks = demoCommunity.getChainByUser(publicKey)
        val items = blocks.map {
            BlockItem(it)
        }
        adapter.updateItems(items)
        imgNoBlocks.isVisible = items.isEmpty()
    }
}
