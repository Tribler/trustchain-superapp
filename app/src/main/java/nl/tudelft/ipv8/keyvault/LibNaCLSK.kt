package nl.tudelft.ipv8.keyvault

import org.libsodium.jni.Sodium
import org.libsodium.jni.crypto.Random

class LibNaCLSK(
    private val privateKey: ByteArray,
    private val signSeed: ByteArray
) : PrivateKey {
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
        return LibNaCLPK(publicKey, verifyKey)
    }

    override fun keyToBin(): String {
        return "LibNaCLSK:$privateKey$signSeed"
    }

    companion object {
        fun generate(): LibNaCLSK {
            val publicKey = ByteArray(Sodium.crypto_box_curve25519xsalsa20poly1305_publickeybytes())
            val privateKey = ByteArray(Sodium.crypto_box_curve25519xsalsa20poly1305_secretkeybytes())
            Sodium.crypto_box_curve25519xsalsa20poly1305_keypair(publicKey, privateKey)

            val signSeed = Random().randomBytes(Sodium.crypto_sign_ed25519_seedbytes())

            return LibNaCLSK(privateKey, signSeed)
        }
    }
}
