package com.example.musicdao

import org.junit.Ignore
import org.junit.Test
import java.io.InputStream
import java.net.URL

class FaucetEndpointTest {
    val id = "abc123xyz"
    val endpointAddress = "http://134.122.59.107:3000"

    @Test
    @Ignore("Unreliable tests") // unit test should not depend on external server
    fun getCoins() {
        val obj = URL("$endpointAddress?id=$id")
        val con: InputStream? = obj.openStream()
        con?.close()
    }
}
