package nl.tudelft.trustchain.debug.utp

import android.util.Log
import net.utp4j.channels.futures.UtpReadListener
import net.utp4j.examples.SaveFileListener
import nl.tudelft.trustchain.debug.UtpTestFragment
import java.security.MessageDigest

class RawResourceListener : UtpReadListener() {
    override fun actionAfterReading() {
        if (exception == null && byteBuffer != null) {
            try {
                byteBuffer.flip()

                // Print the received text file data
                val data = ByteArray(UtpTestFragment.BUFFER_SIZE)
                StringBuffer().apply {
                    while (byteBuffer.hasRemaining()) {
                        append(byteBuffer.get().toInt().toChar())
                    }
                    Log.d("uTP Server", "Received data: $this")
                }
                Log.d("uTP Server", "Received data: ${String(data)}")

                // Save the received data to a file


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getThreadName(): String = "RawResourceListenerThread"
}
