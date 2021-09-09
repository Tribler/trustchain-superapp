package com.example.musicdao.ipv8

import com.example.musicdao.ipv8.SwarmHealth.Deserializer.KEEP_TIME_HOURS
import com.frostwire.jlibtorrent.Sha1Hash
import org.junit.Assert
import org.junit.Test
import java.util.*

class SwarmHealthTest {
    @Test
    fun mergeMaps() {
        val map1 = mutableMapOf<Sha1Hash, SwarmHealth>()
        val map2 = mutableMapOf<Sha1Hash, SwarmHealth>()

        map1[Sha1Hash.max()] = SwarmHealth(Sha1Hash.max().toString(), 1.toUInt(), 1.toUInt())
        map2[Sha1Hash.max()] = SwarmHealth(Sha1Hash.max().toString(), 1.toUInt(), 1.toUInt())
        map2[Sha1Hash.min()] = SwarmHealth(Sha1Hash.min().toString(), 1.toUInt(), 1.toUInt())

        val map3 = map1 + map2
        Assert.assertEquals(map3.size, 2)
    }

    @Test
    fun compare() {
        val a = SwarmHealth(Sha1Hash.max().toString(), 1.toUInt(), 0.toUInt())
        val b = SwarmHealth(Sha1Hash.max().toString(), 0.toUInt(), 1.toUInt())
        val c = SwarmHealth(Sha1Hash.max().toString(), 0.toUInt(), 0.toUInt())

        Assert.assertFalse(a > b)
        Assert.assertFalse(b > a)
        Assert.assertTrue(a > c)
        Assert.assertTrue(b > c)

        Assert.assertEquals(SwarmHealth.pickBest(a, c), a)
        Assert.assertEquals(SwarmHealth.pickBest(a, b), b)
        Assert.assertEquals(SwarmHealth.pickBest(c, b), b)
    }

    @Test
    fun serializeDeserialize() {
        val a = SwarmHealth(Sha1Hash.max().toString(), 1.toUInt(), 0.toUInt())
        val serialized = a.serialize()
        val (b, _) = SwarmHealth.deserialize(serialized)
        Assert.assertEquals(a, b)
    }

    @Test
    fun notEquals() {
        val a = SwarmHealth(
            Sha1Hash.max().toString(),
            1.toUInt(),
            0.toUInt(),
            Date().time.toULong() - 1000.toULong()
        )
        val b =
            SwarmHealth(Sha1Hash.max().toString(), 1.toUInt(), 0.toUInt(), Date().time.toULong())
        Assert.assertNotEquals(a, b)
    }

    @Test
    fun isUpToDate() {
        val a = SwarmHealth(Sha1Hash.max().toString(), 1.toUInt(), 0.toUInt())
        Assert.assertTrue(a.isUpToDate())
        val oldDate = Date().time - 2 * 3600 * KEEP_TIME_HOURS * 1000
        val b = SwarmHealth(Sha1Hash.max().toString(), 1.toUInt(), 0.toUInt(), oldDate.toULong())
        Assert.assertFalse(b.isUpToDate())
    }
}
