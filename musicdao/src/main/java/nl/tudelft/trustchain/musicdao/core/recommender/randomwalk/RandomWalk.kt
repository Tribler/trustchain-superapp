package nl.tudelft.trustchain.musicdao.core.recommender.randomwalk

import mu.KotlinLogging
class RandomWalk<T> (
    var walkSequence: MutableList<T> = mutableListOf()
) {
    private val logger = KotlinLogging.logger {}

    fun addElement(elem: T) {
        walkSequence.add(elem)
    }

}
