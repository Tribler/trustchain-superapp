package nl.tudelft.ipv8.keyvault

import com.goterl.lazycode.lazysodium.LazySodium
import nl.tudelft.ipv8.util.toHex
import kotlin.random.Random

class LibNaClSK(
    val privateKey: ByteArray,
    val signSeed: ByteArray,
    private val lazySodium: LazySodium
) : PrivateKey {
    private val publicKey: ByteArray

    private val signKey: ByteArray
    private val verifyKey: ByteArray

    init {
        publicKey = ByteArray(PUBLICKEY_BYTES)
        lazySodium.cryptoScalarMultBase(publicKey, privateKey)

        verifyKey = ByteArray(SIGN_PUBLICKEY_BYTES)
        signKey = ByteArray(SIGN_SECRETKEY_BYTES)
        lazySodium.cryptoSignSeedKeypair(verifyKey, signKey, signSeed)
    }

    override fun sign(msg: ByteArray): ByteArray {
        val signature = ByteArray(SIGNATURE_SIZE)
        lazySodium.cryptoSignDetached(
            signature,
            msg,
            msg.size.toLong(),
            signKey
        )
        return signature
    }

    override fun pub(): PublicKey {
        return LibNaClPK(publicKey, verifyKey, lazySodium)
    }

    override fun keyToBin(): ByteArray {
        return BIN_PREFIX.toByteArray(Charsets.US_ASCII) + privateKey + signSeed
    }

    override fun toString(): String {
        return keyToHash().toHex()
    }

    override fun equals(other: Any?): Boolean {
        return other is LibNaClSK && keyToHash().contentEquals(other.keyToHash())
    }

    override fun hashCode(): Int {
        return keyToHash().hashCode()
    }

    companion object {
        const val BIN_PREFIX = "LibNaCLSK:"
        const val PUBLICKEY_BYTES = 32
        const val PRIVATEKEY_BYTES = 32
        const val SIGN_PUBLICKEY_BYTES = 32
        const val SIGN_SECRETKEY_BYTES = 64
        const val SIGNATURE_SIZE = 64
        const val SIGN_SEED_BYTES = 32

        fun generate(lazySodium: LazySodium): LibNaClSK {
            val publicKey = ByteArray(PUBLICKEY_BYTES)
            val privateKey = ByteArray(PRIVATEKEY_BYTES)
            lazySodium.cryptoBoxKeypair(publicKey, privateKey)

            val signSeed = Random.nextBytes(SIGN_SEED_BYTES)

            return LibNaClSK(privateKey, signSeed, lazySodium)
        }

        fun fromBin(bin: ByteArray, lazySodium: LazySodium): LibNaClSK {
            val privateKeySize = PRIVATEKEY_BYTES
            val signSeedSize = SIGN_SEED_BYTES
            val binSize = BIN_PREFIX.length + privateKeySize + signSeedSize

            val str = bin.toString(Charsets.US_ASCII)
            val binPrefix = str.substring(0, BIN_PREFIX.length)
            if (binPrefix != BIN_PREFIX)
                throw IllegalArgumentException("Bin prefix $binPrefix does not match $BIN_PREFIX")

            if (bin.size != binSize)
                throw IllegalArgumentException("Bin is expected to have $binSize bytes, has ${bin.size}")

            val privateKey = bin.copyOfRange(BIN_PREFIX.length, BIN_PREFIX.length + privateKeySize)
            val signSeed = bin.copyOfRange(
                BIN_PREFIX.length + privateKeySize,
                BIN_PREFIX.length + privateKeySize + signSeedSize)
            return LibNaClSK(privateKey, signSeed, lazySodium)
        }
    }
}
