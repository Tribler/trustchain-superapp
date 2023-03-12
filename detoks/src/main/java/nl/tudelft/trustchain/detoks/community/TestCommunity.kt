package nl.tudelft.trustchain.detoks.community
import android.util.Log
import nl.tudelft.ipv8.Community
import android.content.Context
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.trustchain.detoks.db.TestBase

class TestCommunity(
    private val database: TestBase,
    private val context: Context
) : Community() {
    override val serviceId = "12313685c1912a191279f8248fc8db5899c5df6a"
    private val MESSAGE_ID = 1

    class Factory(
        private val database: TestBase,
        private val context: Context
    ): Overlay.Factory<TestCommunity>(TestCommunity::class.java){

        override fun create(): TestCommunity {
            return TestCommunity(database, context)
        }
    }

}

