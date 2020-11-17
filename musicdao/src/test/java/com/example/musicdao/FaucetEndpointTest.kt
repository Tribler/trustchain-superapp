package com.example.musicdao

import org.junit.Test
import java.io.InputStream
import java.net.URL

class FaucetEndpointTest {
    val id = "abc123xyz"
    val endpointAddress = "http://134.122.59.107:3000"

    @Test
    fun getCoins() {
        val obj = URL("$endpointAddress?id=$id")
        val con: InputStream? = obj.openStream()
        con?.close()
    }
}
