package com.example.musicdao.net

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable

class SongMessage(val message: String) : Serializable {
    override fun serialize(): ByteArray {
        return message.toByteArray()
    }

    companion object Deserializer : Deserializable<SongMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<SongMessage, Int> {
            return Pair(SongMessage(buffer.toString(Charsets.UTF_8)), buffer.size)
        }
    }
}
