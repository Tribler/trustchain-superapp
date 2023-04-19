package nl.tudelft.trustchain.detoks.benchmark

import nl.tudelft.ipv8.keyvault.PrivateKey

class BasicBlock (
    private val type: String,
    private val message: ByteArray,
    private val senderPublicKey: ByteArray,
    val receiverPublicKey: ByteArray,
) {
    private var signature: ByteArray = "".toByteArray()

    fun sign(key: PrivateKey) {
        signature = key.sign(type.toByteArray() + message + senderPublicKey)
    }
}
