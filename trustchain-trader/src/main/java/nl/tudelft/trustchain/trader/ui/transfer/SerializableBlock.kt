package nl.tudelft.trustchain.trader.ui.transfer

import kotlinx.serialization.Serializable

@Serializable
data class SerializableBlock(val publicKey: String,
                             val sequenceNumber: Int,
                             val previousHash: ByteArray,
                             val signature: ByteArray,
                             val transaction: ByteArray,
                             val timeStamp: Long
)
