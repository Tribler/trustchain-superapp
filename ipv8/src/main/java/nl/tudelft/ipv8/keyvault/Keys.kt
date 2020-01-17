package nl.tudelft.ipv8.keyvault

import nl.tudelft.ipv8.util.sha1

/**
 * Interface for a public or private key.
 */
interface Key {
    /**
     * Get the public key part of the key.
     */
    fun pub(): PublicKey

    /**
     * Get the byte string representation of this key.
     */
    fun keyToBin(): ByteArray

    /**
     * Returns the hash of the public key.
     */
    fun keyToHash(): ByteArray {
        return sha1(pub().keyToBin())
    }
}

/**
 * Interface for a private key.
 */
interface PrivateKey : Key {
    /**
     * Create a signature for a message.
     *
     * @param msg The message to sign.
     * @return The signature for the message.
     */
    fun sign(msg: ByteArray): ByteArray
}

/**
 * Interface for a public key.
 */
interface PublicKey : Key {
    override fun pub() = this

    /**
     * Verify whether a given signature is correct for a message.
     *
     * @param signature The given signature.
     * @param msg The given message.
     * @return True if the signature is valid, false otherwise.
     */
    fun verify(signature: ByteArray, msg: ByteArray): Boolean

    /**
     * Returns the length, in bytes, of each signature made using EC.
     */
    fun getSignatureLength(): Int
}
