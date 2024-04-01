package nl.tudelft.trustchain.debug

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.utp.UtpEndpoint
import nl.tudelft.ipv8.messaging.utp.UtpEndpoint.Companion.BUFFER_SIZE
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.debug.databinding.FragmentUtpTestBinding
import nl.tudelft.trustchain.debug.databinding.PeerComponentBinding
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.random.Random


class UtpTestFragment : BaseFragment(R.layout.fragment_utp_test) {
    private val binding by viewBinding(FragmentUtpTestBinding::bind)

    private val endpoint: UtpEndpoint? = getDemoCommunity().endpoint.utpEndpoint

    private val peers : MutableList<Peer> = mutableListOf()

    private val viewToPeerMap: MutableMap<View, Peer> = mutableMapOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // create list of peers
        getPeers()
        for (peer in peers) {
            println("Adding peer " + peer.toString())
            val layoutInflater: LayoutInflater = this.context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val v: View = layoutInflater.inflate(R.layout.peer_component, null)

            val peerComponentBinding = PeerComponentBinding.bind(v)
            peerComponentBinding.peerIP.setText(peer.address.ip)
            peerComponentBinding.peerPublicKey.setText(peer.publicKey.toString().substring(0, 6))

            viewToPeerMap[v] = peer

            binding.peerListLayout.addView(v, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }


        // IP Selection
//        binding.IPvToggleSwitch.setOnClickListener {
//            if (binding.IPvToggleSwitch.isChecked) {
//                // IPv8 peers
//                binding.IPvSpinner.isEnabled = true
//                binding.editIPforUTP.isEnabled = false
//
//
//                binding.IPvSpinner.adapter = ArrayAdapter(
//                    it.context,
//                    android.R.layout.simple_spinner_item,
//                    peers
//                )
//            } else {
//                // Usual IPv4 address
//                binding.IPvSpinner.isEnabled = false
//                binding.editIPforUTP.isEnabled = true
//
//                // Placeholder data
//                binding.editIPforUTP.text = Editable.Factory.getInstance().newEditable("192.168.0.101:13377")
//
//            }
//        }

        // Data selection
        // Named resources
        val namedFiles = listOf(
            object {
                val name: String = "votes3.csv"
                val id: Int = R.raw.votes3
                override fun toString(): String = name
            },
            object {
                val name: String = "votes13.csv"
                val id: Int = R.raw.votes3
                override fun toString(): String = name
            }
        )

        binding.DAOToggleSwitch.setOnClickListener {
            if (binding.DAOToggleSwitch.isChecked) {
                // Use CSV files
                binding.DAOSpinner.isEnabled = true
                binding.editDAOText.isEnabled = false

                // Hardcoded files
                val files = ArrayAdapter(it.context, android.R.layout.simple_spinner_item, namedFiles)
                binding.DAOSpinner.adapter = files
                binding.DAOSpinner.setSelection(0)
            } else {
                // Use random data
                binding.DAOSpinner.isEnabled = false
                binding.editDAOText.isEnabled = true
            }
        }

        binding.receiveTestPacket.setOnClickListener {
            lifecycleScope.launchWhenCreated {
                fetchData()
            }

        }

        binding.sendTestPacket.setOnClickListener {
//            val address = binding.editIPforUTP.text.toString().split(":")
//            if (address.size == 2) {
//                val ip = address[0]
//                val port = address[1].toIntOrNull() ?: MIN_PORT
//
//                lifecycleScope.launchWhenCreated {
//                    sendTestData(ip, port)
//                }
//            }
        }

        // Send data after clicking on peer component.
        for (peer in binding.peerListLayout.children.iterator()) {
            peer.setOnClickListener {
                val address = viewToPeerMap[peer]?.address.toString().split(":")
                if (address.size == 2) {
                    val ip = address[0]
                    val port = 13377 // address[1].toIntOrNull() ?: MIN_PORT
                    lifecycleScope.launchWhenCreated {
                        sendTestData(ip, port)
                    }
                }
                println("sending data to peer $address")
                updatePeerStatus(viewToPeerMap[peer])

            }
        }
    }

    private fun fetchData() {

        val data: String

        if (endpoint?.listener!!.queue.size > 0) {
            data = String(endpoint.listener.queue.removeFirst())
            Log.d("uTP Client", "Received data!")
        } else {
            data = "No data received!"
            Log.d("uTP Client", "No data received!")
        }

        view?.post {
//            val time = (endTime - startTime) / 1000
//            val speed = Formatter.formatFileSize(requireView().context, (BUFFER_SIZE / time))
            binding.logUTP.text = data.takeLast(2000)
        }
    }


    private fun sendTestData(ip: String, port: Int) {

        val csv3 = resources.openRawResource(R.raw.votes3)
        val csv13 = resources.openRawResource(R.raw.votes13)

//            // 100 MB of random bytes + hash
//            val buffer = generateRandomDataBuffer()
//            // Send CSV file
//            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
//            buffer.put(csv3.readBytes())
        Log.d("uTP Client", "Sending data to $ip:$port")
        getDemoCommunity().endpoint.utpEndpoint?.send(IPv4Address(ip, port), csv3.readBytes())
        csv3.close()
        csv13.close()

    }

    private fun getPeers() {
        Log.d("uTP Client", "Start peer discovery!")
        lifecycleScope.launchWhenCreated {
            val freshPeers = getDemoCommunity().getPeers()
            peers.clear()
            peers.addAll(freshPeers)
            Log.d("uTP Client", "Found ${peers.size} peers! ($peers)")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_utp_test, container, false)
    }

    private fun updatePeerStatus(peer: Peer?) {
        val statusIndicator = findStatusIndicator(peer)
        // Change status indicator depending on peer status.
        // statusIndicator.setBackgroundResource(R.drawable.indicator_yellow)
    }

    private fun findStatusIndicator(peer: Peer?): View {
        // Find the status indicator in the UI for this peer
        val peerLayout = viewToPeerMap.entries.find { it.value == peer }?.key ?: error("Layout for peer $peer not found")
        val statusIndicator = peerLayout?.findViewById<View>(R.id.peerStatusIndicator) ?: error("Status indicator in layout for peer $peer not found")
        return statusIndicator
    }

    private fun generateRandomDataBuffer(): ByteBuffer {
        Log.d("uTP Client", "Start preparing buffer!")
        val rngByteArray = ByteArray(BUFFER_SIZE + 32);
        Random.nextBytes(rngByteArray, 0, BUFFER_SIZE)
        Log.d("uTP Client", "Fill random bytes!")
        // Create hash to check correctness
        Log.d("uTP Client", "Create hash!")
        val buffer = ByteBuffer.wrap(rngByteArray)
        // Create hash to check correctness
        Log.d("uTP Client", "Create hash!")
        val hash = MessageDigest.getInstance("SHA-256").digest(rngByteArray)
        buffer.position(BUFFER_SIZE)
        buffer.put(hash)
        Log.d("uTP Client", "Generated random data with hash $hash")
        return buffer
    }

    companion object {
        const val MIN_PORT = 1024
    }

}
