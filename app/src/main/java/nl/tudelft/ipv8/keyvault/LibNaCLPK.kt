package nl.tudelft.ipv8.keyvault

import org.libsodium.jni.Sodium

open class LibNaCLPK(
    private val publicKey: ByteArray,
    private val verifyKey: ByteArray
) : PublicKey {
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

    override fun keyToBin(): String {
        return "LibNaCLPK:$publicKey$verifyKey"
    }
}
