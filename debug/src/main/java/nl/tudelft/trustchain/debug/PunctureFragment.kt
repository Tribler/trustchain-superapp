package nl.tudelft.trustchain.debug

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.debug.databinding.FragmentPunctureBinding
import nl.tudelft.trustchain.debug.databinding.FragmentWanLogBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PunctureFragment : BaseFragment(R.layout.fragment_puncture) {
    private val binding by viewBinding(FragmentPunctureBinding::bind)

    private var sent = 0
    private var received = 0

    private val receivedMap = mutableMapOf<String, Int>()
    private val firstMessageTimestamps = mutableMapOf<String, Date>()
    private var firstSentMessageTimestamp: Date? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            getDemoCommunity().punctureChannel.asFlow().collect { (peer, payload) ->
                Log.i("PunctureFragment", "Received puncture from ${peer} on port ${payload.identifier}")
                received++
                receivedMap[peer.toString()] = (receivedMap[peer.toString()] ?: 0) + 1
                if (firstMessageTimestamps[peer.toString()] == null) {
                    firstMessageTimestamps[peer.toString()] = Date()
                }
                updateView()
            }
        }

        binding.btnPuncture.setOnClickListener {
            val address = binding.edtAddress.text.toString().split(":")
            if (address.size == 2) {
                val ip = address[0]
                val port = address[1].toIntOrNull() ?: MIN_PORT

                lifecycleScope.launchWhenCreated {
                    firstSentMessageTimestamp = Date()
                    if (binding.sweep.isChecked) {
                        punctureAll(ip)
                    } else {
                        punctureSingle(ip, port)
                    }
                }
            }
        }
    }

    /*
    private suspend fun punctureMultiple(ip: String, port: Int) {
        Log.d("PunctureFragment", "Puncture multiple with initial port: $ip $port")
        val minPort = max(port - SEARCH_BREADTH/2, MIN_PORT)
        val maxPort = min(port + SEARCH_BREADTH/2, MAX_PORT)
        for (i in  minPort .. maxPort) {
            val ipv4 = IPv4Address(ip, i)
            getDemoCommunity().sendPuncture(ipv4, i)
            sent++
            updateView()

            if (i % 10 == 0) {
                delay(40)
            }
        }
    }
    */

    private suspend fun punctureAll(ip: String) {
        while (true) {
            for (i in MIN_PORT..MAX_PORT) {
                val ipv4 = IPv4Address(ip, i)
                getDemoCommunity().sendPuncture(ipv4, i)
                sent++
                updateView()

                if (i % 10 == 0) {
                    delay(40)
                }
                if (i % 1000 == 0) {
                    delay(1000)
                }
            }
        }
    }


    private suspend fun punctureSingle(ip: String, port: Int) {
        while (true) {
            val ipv4 = IPv4Address(ip, port)
            getDemoCommunity().sendPuncture(ipv4, port)
            sent++
            updateView()

            delay(1000)
        }
    }

    private fun updateView() {
        val df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM)
        binding.txtResult.text = "Sent: $sent\nFirst Sent: $firstSentMessageTimestamp\nReceived: $received\n\n" + receivedMap.map {
            val date = firstMessageTimestamps[it.key]
            val time = if (date != null) df.format(date) else null
            it.key + " (" + time + ") -> " + it.value
        }.joinToString("\n")
    }

    companion object {
        const val MIN_PORT = 1024
        const val MAX_PORT = 65535
        const val SEARCH_BREADTH = 1000
    }
}
