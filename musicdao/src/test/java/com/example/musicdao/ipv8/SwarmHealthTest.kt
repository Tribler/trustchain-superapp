package com.example.musicdao.ipv8

import com.frostwire.jlibtorrent.Sha1Hash
import org.junit.Assert
import org.junit.Test

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
}
