package nl.tudelft.trustchain.common.ebsi

import nl.tudelft.ipv8.util.hexToBytes
import org.bitcoinj.core.ECKey

class ECIESPayload(
    val iv: ByteArray,
    val ephemPublickKey: ECKey,
    val mac: ByteArray,
    val ciphertext: ByteArray
) {

    companion object {

        fun parseCiphertext(encCiphertext: String): ECIESPayload{
            val data = encCiphertext.hexToBytes()
            val iv = data.copyOfRange(0, 16)
            val ephemPublicKey = data.copyOfRange(16, 49)
            val mac = data.copyOfRange(49, 81)
            val ciphertext = data.copyOfRange(81, data.size)

            val decompressedEphemPublicKey = publicKeyConvert(ephemPublicKey)
            return ECIESPayload(iv, decompressedEphemPublicKey, mac, ciphertext)
        }

        private fun publicKeyConvert(publicKeyData: ByteArray): ECKey {
            return ECKey.fromPublicOnly(publicKeyData).decompress()
        }
    }
}
