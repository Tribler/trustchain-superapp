package nl.tudelft.ipv8.keyvault

import com.goterl.lazycode.lazysodium.LazySodium
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
        val prefix = "LibNaCLSK:"
        return prefix.toByteArray(Charsets.US_ASCII) + privateKey + signSeed
    }

    companion object {
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
    }
}
