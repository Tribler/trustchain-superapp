package nl.tudelft.ipv8.util

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
