package nl.tudelft.ipv8.keyvault

import org.libsodium.jni.NaCl
import org.libsodium.jni.Sodium
import org.libsodium.jni.crypto.Random

class LibNaClSK(
    val privateKey: ByteArray,
    val signSeed: ByteArray
) : PrivateKey {
    init {
        NaCl.sodium()
    }

    private val publicKey: ByteArray

    private val signKey: ByteArray
    private val verifyKey: ByteArray

    init {
        assert(privateKey.size == Sodium.crypto_scalarmult_curve25519_scalarbytes())

        publicKey = ByteArray(Sodium.crypto_scalarmult_curve25519_bytes())
        Sodium.crypto_scalarmult_curve25519_base(publicKey, privateKey)

        verifyKey = ByteArray(Sodium.crypto_sign_ed25519_publickeybytes())
        signKey = ByteArray(Sodium.crypto_sign_ed25519_secretkeybytes())
        Sodium.crypto_sign_seed_keypair(verifyKey, signKey, signSeed)
    }

    override fun sign(msg: ByteArray): ByteArray {
        val signature = ByteArray(Sodium.crypto_sign_bytes())
        Sodium.crypto_sign_ed25519_detached(
            signature,
            intArrayOf(signature.size),
            msg,
            msg.size,
            signKey
        )
        return signature
    }

    override fun pub(): PublicKey {
        return LibNaClPK(publicKey, verifyKey)
    }

    override fun keyToBin(): ByteArray {
        val prefix = "LibNaCLSK:"
        return prefix.toByteArray(Charsets.US_ASCII) + privateKey + signSeed
    }

    companion object {
        init {
            NaCl.sodium()
        }

        fun generate(): LibNaClSK {
            val publicKey = ByteArray(Sodium.crypto_box_curve25519xsalsa20poly1305_publickeybytes())
            val privateKey = ByteArray(Sodium.crypto_box_curve25519xsalsa20poly1305_secretkeybytes())
            Sodium.crypto_box_curve25519xsalsa20poly1305_keypair(publicKey, privateKey)

            val signSeed = Random().randomBytes(Sodium.crypto_sign_ed25519_seedbytes())

            return LibNaClSK(privateKey, signSeed)
        }
    }
}
