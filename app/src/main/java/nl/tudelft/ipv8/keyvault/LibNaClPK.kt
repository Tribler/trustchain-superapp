package nl.tudelft.ipv8.keyvault

import nl.tudelft.ipv8.util.toHex
import org.libsodium.jni.NaCl
import org.libsodium.jni.Sodium

open class LibNaClPK(
    private val publicKey: ByteArray,
    private val verifyKey: ByteArray
) : PublicKey {
    init {
        NaCl.sodium()
    }

    override fun verify(signature: ByteArray, msg: ByteArray): Boolean {
        return Sodium.crypto_sign_ed25519_verify_detached(
            signature,
            msg,
            msg.size,
            verifyKey
        ) == 0
    }

    override fun getSignatureLength(): Int {
        return Sodium.crypto_sign_bytes()
    }

    override fun keyToBin(): ByteArray {
        return BIN_PREFIX.toByteArray(Charsets.US_ASCII) + publicKey + verifyKey
    }

    companion object {
        private const val BIN_PREFIX = "LibNaCLPK:"

        init {
            NaCl.sodium()
        }

        /**
         * Creates a public key from a hex-encoded binary string.
         *
         * @throws IllegalArgumentException If the argument does not represent a valid public key.
         */
        fun fromBin(bin: ByteArray): LibNaClPK {
            val publicKeySize = Sodium.crypto_scalarmult_curve25519_bytes()
            val verifyKeySize = Sodium.crypto_sign_ed25519_publickeybytes()
            val binSize = BIN_PREFIX.length + publicKeySize + verifyKeySize

            val str = bin.toString(Charsets.US_ASCII)
            val binPrefix = str.substring(0, BIN_PREFIX.length)
            if (binPrefix != BIN_PREFIX)
                throw IllegalArgumentException("Bin prefix $binPrefix does not match $BIN_PREFIX")

            if (bin.size != binSize)
                throw IllegalArgumentException("Bin is expected to have $binSize bytes, has ${bin.size}")

            val publicKey = bin.copyOfRange(BIN_PREFIX.length, BIN_PREFIX.length + publicKeySize)
            val verifyKey = bin.copyOfRange(BIN_PREFIX.length + publicKeySize,
                BIN_PREFIX.length + publicKeySize + verifyKeySize)
            return LibNaClPK(publicKey, verifyKey)
        }
    }

    override fun toString(): String {
        return keyToHash().toHex()
    }
}
