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
import androidx.activity.addCallback
import androidx.navigation.findNavController
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.DeToksCommunity
import nl.tudelft.trustchain.detoks.R
import nl.tudelft.trustchain.detoks.adapters.NetworkAdapter

class NetworkFragment : Fragment() {
    private lateinit var networkLabel: TextView
    private lateinit var adapter: ArrayAdapter<String>

    private fun update() {
        val peers = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!.getPeers().map { it.publicKey.toString() }

        networkLabel.text = "Network: " + peers.size.toString()

        if (!adapter.isEmpty) adapter.clear()
        adapter.addAll(peers)
        adapter.notifyDataSetChanged()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        networkLabel = view.findViewById(R.id.networkLabel)

        val reloadButton = view.findViewById<ImageButton>(R.id.reloadButton)
        reloadButton.setOnClickListener {
            update()
        }

        adapter = NetworkAdapter(requireActivity(), arrayListOf())
        update()

        val peersList = view.findViewById<ListView>(R.id.peersList)
        peersList.adapter = adapter
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

        requireActivity().onBackPressedDispatcher.addCallback {
            view.findNavController().navigate(NetworkFragmentDirections.actionNetworkFragmentToTabBarFragment())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_network, container, false)
    }
}
