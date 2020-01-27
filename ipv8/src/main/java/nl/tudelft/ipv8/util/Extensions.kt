package nl.tudelft.ipv8.util

fun ByteArray?.contentEquals(other: ByteArray?): Boolean {
    return this != null && other != null && contentEquals(other)
}
