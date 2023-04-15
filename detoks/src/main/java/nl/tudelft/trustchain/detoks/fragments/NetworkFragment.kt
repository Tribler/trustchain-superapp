package nl.tudelft.trustchain.detoks.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.findNavController
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.DeToksCommunity
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.adapters.NetworkAdapter

class NetworkFragment : Fragment() {
    private lateinit var networkLabel: TextView
    private lateinit var backButton: ImageButton
    private lateinit var reloadButton: ImageButton
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var peersList: ListView

    private fun update() {
        val peers = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!.getPeers().map { it.publicKey.toString() }

        networkLabel.text = "Network: " + peers.size.toString()

        if (!adapter.isEmpty) adapter.clear()
        adapter.addAll(peers)
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        networkLabel = view.findViewById(R.id.networkLabel)
        backButton = view.findViewById(R.id.backButton)
        reloadButton = view.findViewById(R.id.reloadButton)

        adapter = NetworkAdapter(requireActivity(), arrayListOf())
        peersList = view.findViewById(R.id.peersList)
        peersList.adapter = adapter

        backButton.setOnClickListener {
            it.findNavController().navigate(NetworkFragmentDirections.actionNetworkFragmentToTabBarFragment())
        }

        reloadButton.setOnClickListener {
            update()
        }

        peersList.setOnItemLongClickListener { _, _, i,_ ->
            val clipboardManager = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val data = adapter.getItem(i)
            if (data != null) {
                val clipData = ClipData.newPlainText("text", data)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(this.requireContext(), "Text copied to clipboard.", Toast.LENGTH_LONG).show()
            }
            true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_network, container, false)
    }
}
