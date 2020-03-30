package com.example.musicdao.net

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.messaging.Packet

class MusicDemoCommunity: Community() {
    override val serviceId = "29384902d2938f34872398758cf7ca9238ccc333"

    fun broadcastGreeting() {
        for (peer in getPeers()) {
            val packet = serializePacket(Companion.MESSAGE_ID, SongMessage("Song ID: 12345"))
            send(peer.address, packet)
        }
    }

    companion object {
        private const val MESSAGE_ID = 1
    }
}
