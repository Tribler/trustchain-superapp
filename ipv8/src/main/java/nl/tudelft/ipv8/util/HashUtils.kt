package nl.tudelft.ipv8.util

import java.security.MessageDigest

private const val SHA1 = "SHA-1"
private const val SHA256 = "SHA-256"

fun sha1(input: ByteArray): ByteArray {
    return MessageDigest
        .getInstance(SHA1)
        .digest(input)
}

fun sha256(input: ByteArray): ByteArray {
    return MessageDigest
        .getInstance(SHA256)
        .digest(input)
}
