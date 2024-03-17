package nl.tudelft.trustchain.debug.utp

import android.util.Log
import net.utp4j.channels.futures.UtpReadListener
import nl.tudelft.trustchain.debug.UtpTestFragment
import java.security.MessageDigest

class BaseDataListener : UtpReadListener() {
    override fun actionAfterReading() {
        if (exception == null && byteBuffer != null) {
            try {
                byteBuffer.flip()
                // Unpack received hash
                val receivedHashData = ByteArray(32)
                val data = ByteArray(UtpTestFragment.BUFFER_SIZE + 32)
                byteBuffer.get(data, 0, UtpTestFragment.BUFFER_SIZE)
                byteBuffer.get(receivedHashData)

                // Hash the received data
                val hash = MessageDigest.getInstance("SHA-256").digest(data)

                if (MessageDigest.isEqual(hash, receivedHashData)) {
                    Log.d("uTP Server", "Correct hash received")
                } else {
                    Log.d("uTP Server", "Invalid hash received!")
                }

                // Display the received data

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getThreadName(): String = "BaseDataListenerThread"
}
