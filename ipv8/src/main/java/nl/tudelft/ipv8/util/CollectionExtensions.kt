package nl.tudelft.ipv8.util

fun <E> Collection<E>.random(maxSampleSize: Int): Collection<E> {
    val sampleSize = kotlin.math.min(size, maxSampleSize)
    return shuffled().subList(0, sampleSize)
}
