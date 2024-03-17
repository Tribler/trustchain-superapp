package nl.tudelft.trustchain.debug

import android.os.Bundle
import android.text.Editable
import android.text.format.Formatter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.utp4j.channels.UtpServerSocketChannel
import net.utp4j.channels.UtpSocketChannel
import net.utp4j.examples.SaveFileListener
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.debug.databinding.FragmentUtpTestBinding
import nl.tudelft.trustchain.debug.utp.BaseDataListener
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.random.Random


class UtpTestFragment : BaseFragment(R.layout.fragment_utp_test) {
    private val binding by viewBinding(FragmentUtpTestBinding::bind)
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val peers : MutableList<Peer> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // IP Selection
        binding.IPvToggleSwitch.setOnClickListener {
            if (binding.IPvToggleSwitch.isChecked) {
                // IPv8 peers
                binding.IPvSpinner.isEnabled = true
                binding.editIPforUTP.isEnabled = false
                getPeers()

                binding.IPvSpinner.adapter = ArrayAdapter(
                    it.context,
                    android.R.layout.simple_spinner_item,
                    peers
                )
            } else {
                // Usual IPv4 address
                binding.IPvSpinner.isEnabled = false
                binding.editIPforUTP.isEnabled = true

                // Placeholder data
                binding.editIPforUTP.text = Editable.Factory.getInstance().newEditable("192.168.0.101:13377")

            }
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

        binding.receiveTestPacket.setOnClickListener {
            val address = binding.editIPforUTP.text.toString().split(":")
            if (address.size == 2) {
                val ip = address[0]
                val port = address[1].toIntOrNull() ?: MIN_PORT

                runServer(ip, port)
            }
        }

        binding.sendTestPacket.setOnClickListener {
            val address = binding.editIPforUTP.text.toString().split(":")
            if (address.size == 2) {
                val ip = address[0]
                val port = address[1].toIntOrNull() ?: MIN_PORT

                lifecycleScope.launchWhenCreated {
                    sendTestData(ip, port)
                }
            }
        }
    }

    private fun runServer(ip: String, port: Int) {
        val buffer = ByteBuffer.allocate(BUFFER_SIZE + 32)

        lifecycleScope.launchWhenCreated {
            scope.launch(Dispatchers.IO) {
                var startTime = 0L;
                var endTime = 0L;
                UtpServerSocketChannel.open().let { server ->
                    server.bind(InetSocketAddress(ip, port))
                    Log.d("uTP Server", "Server started on $ip:$port")
                    server.accept()?.run {
                        block()
                        Log.d("uTP Server", "Receiving new data!")
                        startTime = System.currentTimeMillis()
                        channel.let {
                            it.read(buffer)?.run {
                                setListener(BaseDataListener())
                                block()
                            }
                            endTime = System.currentTimeMillis();
                            Log.d("uTP Server", "Finished receiving data!")
                        }
                        channel.close()
                    }
                    server.close()
                    Log.d("uTP Server", "Stopping the server!")
                }

                // Print contents of buffer
                Log.d("uTP Server", "Buffer contents: ${buffer.array().contentToString()}")

                view?.post {
                    val time = (endTime - startTime) / 1000
                    val speed = Formatter.formatFileSize(requireView().context, (BUFFER_SIZE / time))
                    binding.logUTP.text = "Transfer time: ${time}s \n Avg speed: ${speed}/s"
                }
            }
        }
    }


    private fun sendTestData(ip: String, port: Int) {

        val csv3 = resources.openRawResource(R.raw.votes3)
        val csv13 = resources.openRawResource(R.raw.votes13)

        scope.launch(Dispatchers.IO) {
            // 100 MB of random bytes + hash
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
            Log.d("uTP Client", "Finished preparing buffer!")

            Log.d("uTP Client", "Sending random data with hash $hash")
            UtpSocketChannel.open().let { channel ->
                val future = channel.connect(InetSocketAddress(ip, port))?.apply { block() }
                if (future != null) {
                    if (future.isSuccessfull) {
                        channel.write(buffer)?.apply { block() }
                        Log.d("uTP Client", "Sent buffer")
                    } else
                        Log.e("uTP Client", "Did not manage to connect to the server!")
                } else {
                    Log.e("uTP Client", "Future is null!")
                }
                channel.close()
            }
        }
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

    companion object {
        const val MIN_PORT = 1024
        const val BUFFER_SIZE = 50
    }

}
