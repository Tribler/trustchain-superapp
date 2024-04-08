package nl.tudelft.trustchain.debug

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

import android.widget.TextView
import android.os.SystemClock
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import net.utp4j.data.UtpPacket
import net.utp4j.data.UtpPacketUtils
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.utp.UtpIPv8Endpoint
import nl.tudelft.ipv8.messaging.utp.UtpIPv8Endpoint.Companion.BUFFER_SIZE
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.debug.databinding.FragmentUtpTestBinding
import nl.tudelft.trustchain.debug.databinding.PeerComponentBinding
import java.net.InetAddress
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.random.Random


class UtpTestFragment : BaseFragment(R.layout.fragment_utp_test) {
    private val binding by viewBinding(FragmentUtpTestBinding::bind)

    private val peers : MutableList<Peer> = mutableListOf()

    private val endpoint: UtpIPv8Endpoint? = getDemoCommunity().endpoint.udpEndpoint?.utpIPv8Endpoint

    private var ipv8Mode : Boolean = false
    private val viewToPeerMap: MutableMap<View, Peer> = mutableMapOf()
    private val logMap : MutableMap<Short, TextView> = HashMap();
    private val connectionInfoMap : MutableMap<Short, ConnectionInfo> = HashMap();

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // create list of peers
        getPeers()

        for (peer in peers) {
            Log.d(LOG_TAG, "Adding peer " + peer.toString())
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

        binding.sendTestPacket.setOnClickListener {
            val myWan = getDemoCommunity().myEstimatedLan
            lifecycleScope.launchWhenCreated {
                sendTestData(myWan.ip, myWan.port)
            }
        }

        // Send data after clicking on peer component.
        for (peerView in binding.peerListLayout.children.iterator()) {
            peerView.setOnClickListener {
                val peer = viewToPeerMap[peerView] ?: error("Peer not found for view")
                val address = peer.address.toString()
                lifecycleScope.launchWhenCreated {
                    sendTestData(peer)
                }
                Log.d(LOG_TAG,"sending data to peer $address")
                updatePeerStatus(peer)
            }
        }

        getDemoCommunity().endpoint.udpEndpoint?.utpIPv8Endpoint?.rawPacketListeners?.add { packet ->
            val utpPacket = UtpPacketUtils.extractUtpPacket(packet)
            if (UtpPacketUtils.isSynPkt(utpPacket)) {
                startConnectionLog(utpPacket.connectionId, packet.address)
            } else if (utpPacket.windowSize == 0) {
                finalizeConnectionLog(utpPacket.connectionId, packet.address)
            } else {
                updateConnectionLog(utpPacket, packet.address)
            }
        }

//        getDemoCommunity().endpoint.udpEndpoint?.addListener(object : EndpointListener {
//            override fun onPacket(packet: Packet) {
//                println("receive udp pkt")
//            }
//
//            override fun onEstimatedLanChanged(address: IPv4Address) {
//                println("lan changes")
//            }
//
//        })
//
//        fixedRateTimer("logUpdateTimer", daemon = true, period = 500, action = {
//            updateConnectionLogs()
//        })
    }

    private fun startConnectionLog(connectionId: Short, source: InetAddress) {
        // do not recreate log for same connection
        if (logMap.containsKey(connectionId)) {
            return
        }

        // store info on connection in map
        connectionInfoMap.put((connectionId + 1).toShort(), ConnectionInfo(source, SystemClock.uptimeMillis(), 0))

        // create new log section in fragment
        activity?.runOnUiThread {
            val logView = TextView(this.context);
            logView.setText(String.format("%s: Connected", source))

            binding.connectionLogLayout.addView(logView)

            synchronized(logMap) {
                logMap.put((connectionId + 1).toShort(), logView)
            }
        }

    }

    private fun updateConnectionLog(utpPacket: UtpPacket, source: InetAddress) {
        // if connection is not know, do nothing
        val connectionId = utpPacket.connectionId

        if (!logMap.containsKey(connectionId)) {
            return
        }

        // update ConnectionInfo
        // TODO: retransmitted packets currently count towards data transferred, but shouldn't
        connectionInfoMap[connectionId]!!.dataTransferred += utpPacket.payload.size

        // display current ack number
        // TODO: too much updating of UI causes frame drop, change to periodic update from render thread
        // temporary solution: do not display every
        if (utpPacket.sequenceNumber % 50 == 0) {
            activity?.runOnUiThread {
                val logView = logMap.get(connectionId)

                logView?.setText(String.format("%s: receiving data, sequence number #%d", source, utpPacket.sequenceNumber))
                logView?.postInvalidate()
            }
        }
    }

    private fun finalizeConnectionLog(connectionId: Short, source: InetAddress) {
        // if connection is not know, do nothing
        if (!logMap.containsKey(connectionId)) {
            return
        }

        val connectionInfo = connectionInfoMap[connectionId]!!;

        // display current ack number
        activity?.runOnUiThread {
            val logView = logMap.get(connectionId)

            val dataTransferred = formatDataTransferredMessage(connectionInfo.dataTransferred);
            val transferTime = (SystemClock.uptimeMillis() - connectionInfo.connectionStartTimestamp).div(1000.0)
            val transferSpeed = formatTransferSpeed(connectionInfo.dataTransferred, transferTime)

            logView?.setText(String.format("%s: transfer completed: received %s in %.2f s (%s)",
                source, dataTransferred, transferTime, transferSpeed))
            logView?.postInvalidate()
        }

    }

    private fun formatDataTransferredMessage(numBytes: Int): String {
        if (numBytes < 1_000) {
            return String.format("%d B", numBytes)
        } else if (numBytes < 1_000_000) {
            return String.format("%.2f KiB", numBytes.div(1_000.0))
        } else {
            return String.format("%.2f MiB", numBytes.div(1_000_000.0))
        }
    }

    private fun formatTransferSpeed(numBytes: Int, time: Double): String {
        val bytesPerSecond = numBytes.div(time)

        if (bytesPerSecond < 1_000) {
            return String.format("%.2f B/s", bytesPerSecond)
        } else if (bytesPerSecond < 1_000_000) {
            return String.format("%.2f KiB/s", bytesPerSecond/1_000)
        } else {
            return String.format("%.2f MiB/s", bytesPerSecond/1_000_000)
        }
    }

    private fun sendTestData(peer: Peer) {
        sendTestData(peer.address.ip, peer.address.port)
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
        getDemoCommunity().endpoint.udpEndpoint?.sendUtp(IPv4Address(ip, port), csv3.readBytes())
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
        const val LOG_TAG = "uTP Debug"
    }

    private data class ConnectionInfo(val source: InetAddress, val connectionStartTimestamp: Long, var dataTransferred: Int)
}
