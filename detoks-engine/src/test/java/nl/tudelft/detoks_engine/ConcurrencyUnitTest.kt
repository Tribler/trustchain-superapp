package nl.tudelft.detoks_engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random


internal class ConcurrencyUnitTest {
    @Test
    fun doTest() {
        val data = genData(10000, 20)
        val actual = ConcurrentHashMap<Int, ConcurrentLinkedQueue<String>>().also { actual ->
            runBlocking {
                data.chunked(data.size / 4).forEach { chunk ->
                    launch {
                        chunk.forEach { block ->
                            actual.getOrPut(block.peerId) { ConcurrentLinkedQueue<String>() }.offer(block.payload)
                        }
                    }
                }
                // magic for simplicity
                val start = System.currentTimeMillis()
                while (System.currentTimeMillis() - start < 2000) {
                    delay(100)
                }
            }
        }
        val expected = data.groupBy({ it.peerId }, { it.payload })
        val keys = expected.keys.toIntArray().sortedArray()
        Assert.assertArrayEquals("Inconsistent peers", keys, actual.keys.toIntArray().sortedArray())
        keys.forEach { key ->
            val e = expected[key].also { Assert.assertNotNull(it) }!!.toTypedArray().sortedArray()
            val a = actual[key].also { Assert.assertNotNull(it) }!!.toTypedArray().sortedArray()
            Assert.assertArrayEquals("Inconsistent payload for peer $key", e, a)
        }
    }


    data class Block(val peerId: Int, val payload: String)


    fun genData(nData: Int, nGroup: Int) = List(nData) {
        Block(Random.nextInt(0, nGroup), UUID.randomUUID().toString())
    }
}
