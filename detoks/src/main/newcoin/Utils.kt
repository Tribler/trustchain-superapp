import java.math.BigInteger
import java.security.*
import java.util.*

fun String.hash(algorithm: String = "SHA-256"): String {
    val messageDigest = MessageDigest.getInstance(algorithm)
    messageDigest.update(this.toByteArray())
    return String.format("%064x", BigInteger(1, messageDigest.digest()))
}

fun String.sign(privateKey: PrivateKey, algorithm: String = "SHA256withRSA") : ByteArray {
    val rsa = Signature.getInstance(algorithm)
    rsa.initSign(privateKey)
    rsa.update(this.toByteArray())
    return rsa.sign()
}

fun String.verifySignature(publicKey: PublicKey, signature: ByteArray, algorithm: String = "SHA256withRSA") : Boolean {
    val rsa = Signature.getInstance(algorithm)
    rsa.initVerify(publicKey)
    rsa.update(this.toByteArray())
    return rsa.verify(signature)
}

fun Key.encodeToString() : String {
    return Base64.getEncoder().encodeToString(this.encoded)
}