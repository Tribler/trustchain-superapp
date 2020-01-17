package nl.tudelft.ipv8.util

import java.security.MessageDigest

private const val SHA1 = "SHA-1"

fun sha1(input: ByteArray): ByteArray {
    return MessageDigest
        .getInstance(SHA1)
        .digest(input)
}
