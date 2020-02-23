package nl.tudelft.ipv8.keyvault

import com.goterl.lazycode.lazysodium.LazySodium
import nl.tudelft.ipv8.util.toHex

class LibNaClPK(
    val publicKey: ByteArray,
    val verifyKey: ByteArray,
    private val lazySodium: LazySodium
) : PublicKey {
    override fun verify(signature: ByteArray, msg: ByteArray): Boolean {
        return lazySodium.cryptoSignVerifyDetached(signature, msg, msg.size, verifyKey)
    }

    override fun getSignatureLength(): Int {
        return LibNaClSK.SIGNATURE_SIZE
    }

    override fun keyToBin(): ByteArray {
        return BIN_PREFIX.toByteArray(Charsets.US_ASCII) + publicKey + verifyKey
    }

    override fun toString(): String {
        return keyToHash().toHex()
    }

    override fun equals(other: Any?): Boolean {
        return other is LibNaClPK && keyToHash().contentEquals(other.keyToHash())
    }

    override fun hashCode(): Int {
        return keyToHash().hashCode()
    }

    companion object {
        private const val BIN_PREFIX = "LibNaCLPK:"

        /**
         * Creates a public key from a hex-encoded binary string.
         *
         * @throws IllegalArgumentException If the argument does not represent a valid public key.
         */
        fun fromBin(bin: ByteArray, lazySodium: LazySodium): LibNaClPK {
            val publicKeySize = LibNaClSK.PUBLICKEY_BYTES
            val verifyKeySize = LibNaClSK.SIGN_PUBLICKEY_BYTES
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
            return LibNaClPK(publicKey, verifyKey, lazySodium)
        }
    }
}
