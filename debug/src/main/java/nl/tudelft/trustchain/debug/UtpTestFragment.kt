package nl.tudelft.trustchain.debug

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.common.hash.HashCode
import com.google.common.hash.Hashing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.utp4j.channels.UtpServerSocketChannel
import net.utp4j.channels.UtpSocketChannel
import net.utp4j.examples.SaveFileListener
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.debug.databinding.FragmentUtpTestBinding
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.random.Random


class UtpTestFragment : Fragment() {
    private val binding by viewBinding(FragmentUtpTestBinding::bind)
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var recHashString: String = "";
    private var localHashString: String = "";
    private var equalityOfHash: Boolean = false;

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.openServer.setOnClickListener {
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
                UtpServerSocketChannel.open().let { server ->
                    server.bind(InetSocketAddress(ip, port))
                    Log.d("uTP Server", "Server started on $ip:$port")
                    server.accept()?.run {
                        block()
                        channel.let {
                            it.read(buffer)?.run {
                                setListener(SaveFileListener())
                                block()
                            }
                            Log.d("uTP Server", "Finished receiving data!")
                        }
                        channel.close()
                    }
                    server.close()
                    Log.d("uTP Server", "Stopping the server!")
                }
                // Unpack received hash
                val receivedHashData = ByteArray(32)
                val data = ByteArray(BUFFER_SIZE + 32)
                buffer.get(data, 0, BUFFER_SIZE)
                buffer.get(receivedHashData)

                val hash = Hashing.sha256().hashBytes(data)
                val receivedHash = HashCode.fromBytes(receivedHashData)
                equalityOfHash = hash.equals(receivedHash)
                recHashString = receivedHash.toString()
                localHashString = hash.toString()

                if (!equalityOfHash) {
                    Log.d("uTP Server", "Invalid hash received!!!")
                } else {
                    Log.d("uTP Server", "Correct hash received")
                }
            }
            // TODO: This should be updated when the data is actually there
            binding.utpLog.text = "Received data with hashes equal ${equalityOfHash} \n ${localHashString} \n ${recHashString}"
        }
    }


    private fun sendTestData(ip: String, port: Int) {
        scope.launch(Dispatchers.IO) {
            // 100 MB of random bytes + hash
            Log.d("uTP Client", "Start preparing buffer!")
            val rngByteArray = ByteArray(BUFFER_SIZE + 32);
            Random.nextBytes(rngByteArray, 0, BUFFER_SIZE)
            Log.d("uTP Client", "Fill random bytes!")
            val buffer = ByteBuffer.wrap(rngByteArray)
            // Create hash to check correctness
            Log.d("uTP Client", "Create hash!")
            val hash = Hashing.sha256().hashBytes(buffer)
            buffer.position(BUFFER_SIZE)
            buffer.put(hash.asBytes())
            Log.d("uTP Client", "Finished preparing buffer!")

            Log.d("uTP Client", "Sending random data with hash ${hash}")
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_utp_test, container, false)
    }

    companion object {
        const val MIN_PORT = 1024
        const val BUFFER_SIZE = 100
    }

}
