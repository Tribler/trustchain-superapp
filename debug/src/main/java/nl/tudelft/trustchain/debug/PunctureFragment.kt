package nl.tudelft.trustchain.debug

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.debug.databinding.FragmentPunctureBinding
import java.text.SimpleDateFormat
import java.util.Date

class PunctureFragment : BaseFragment(R.layout.fragment_puncture) {
    private val binding by viewBinding(FragmentPunctureBinding::bind)

    private var sent = 0
    private var received = 0

    private val receivedMap = mutableMapOf<String, Int>()
    private val firstMessageTimestamps = mutableMapOf<String, Date>()
    private var firstSentMessageTimestamp: Date? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            while (isActive) {
                updateView()
                delay(100)
            }
        }
        lifecycleScope.launchWhenCreated {
            getDemoCommunity().punctureChannel.collect { (peer, payload) ->
                Log.i(
                    "PunctureFragment",
                    "Received puncture from $peer on port ${payload.identifier}"
                )
                received++
                receivedMap[peer.toString()] = (receivedMap[peer.toString()] ?: 0) + 1
                if (firstMessageTimestamps[peer.toString()] == null) {
                    firstMessageTimestamps[peer.toString()] = Date()
                }
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
                        punctureAll(ip, false)
                        delay(30000)
                        punctureAll(ip, true)
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

    private suspend fun punctureAll(
        ip: String,
        slow: Boolean
    ) = with(Dispatchers.Default) {
        for (i in MIN_PORT..MAX_PORT) {
            val ipv4 = IPv4Address(ip, i)
            getDemoCommunity().sendPuncture(ipv4, i)
            sent++

            if (i % 1000 == 0) {
                delay(if (slow) 30000L else 1L)
            }
        }
    }

    private suspend fun punctureSingle(
        ip: String,
        port: Int
    ) {
        while (true) {
            val ipv4 = IPv4Address(ip, port)
            getDemoCommunity().sendPuncture(ipv4, port)
            sent++

            delay(1000)
        }
    }

    private fun updateView() {
        val df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM)
        binding.txtResult.text =
            "Sent: $sent\nFirst Sent: $firstSentMessageTimestamp\nReceived: $received\n\n" +
            receivedMap.map {
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
