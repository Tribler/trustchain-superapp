package nl.tudelft.trustchain.debug.utp

import net.utp4j.channels.futures.UtpReadListener

class RawResourceListener : UtpReadListener() {
    override fun actionAfterReading() {
        TODO("Not yet implemented")
    }



    override fun getThreadName(): String = "RawResourceListenerThread"
}
