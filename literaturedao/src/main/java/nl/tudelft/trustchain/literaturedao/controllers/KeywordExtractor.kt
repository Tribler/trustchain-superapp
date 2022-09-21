package nl.tudelft.trustchain.literaturedao.controllers

import android.util.Log
import nl.tudelft.trustchain.literaturedao.LiteratureDaoActivity
import java.io.InputStream
import nl.tudelft.trustchain.literaturedao.snowball.Main.main as stem


class KeywordExtractor() {

    val hardCodedMagickNumber = 1
    val maxKWs = 99999

    // Function that loads the average stemmed word occurance
    fun instantiateAvgFreqMap(csv: InputStream): Map<String, Long> {

        var res = mutableMapOf<String, Long>()
        csv.bufferedReader().useLines { lines ->
            lines.forEach {
                val key = it.split(",".toRegex())[0]
                val num = it.split(",".toRegex())[1].toLong()
                res[key] = num
            }
        }
        return res
    }

    // Custom data type in order to be able to sort
    class Result constructor(word: String, relativeFreq: Double) {
        val first = word
        val second = relativeFreq
        fun second(): Double {
            return second
        }

        override fun toString(): String {
            val builder = StringBuilder()
            builder.append("(")
                .append(first)
                .append(", ")
                .append(second.toString())
                .append(")")
            return builder.toString()
        }
    }

    fun extract(text: String, csv: InputStream): MutableList<Pair<String, Double>> {

        // Establish general averages
        val avgTotal = 588089694315
        val avgFreqMap = instantiateAvgFreqMap(csv)

        // Append a word to the input as the used library forgets about the last word
        // Stem the input using the snowball library and split into list of words
        var input = stem(text.plus(" bugFix"))
            .split("\\s".toRegex())

        // Instantiate a map for the word counts and a counter for the total words that were recognized
        var freqMap = mutableMapOf<String, Int>()
        var total = 0

        // Count words known in averageFreqMap in input
        for (word in input) {
            val avgFreq = avgFreqMap[word]
            if (avgFreq == null) continue
            total += 1
            if (word !in freqMap.keys) {
                freqMap[word] = 1
            } else {
                freqMap[word] = freqMap.getValue(word) + 1
            }
        }

        // Instantiate a result list
        var relativeFreqList = mutableListOf<Result>()

        //Calculate relative frequencies per word
        for ((word, freq) in freqMap.entries) {
            var avgFreq = avgFreqMap[word]
            if (avgFreq == null) {
                avgFreq = 1
            }
            val relativeFreq =
                (freq.toDouble() / total.toDouble()) / (avgFreq.toDouble() / avgTotal.toDouble())
            // filter for relevance of the word
            if (relativeFreq > hardCodedMagickNumber) {
                relativeFreqList.add(Result(word, relativeFreq))
            }
        }
        // sort results (not working yet)
        relativeFreqList.sortBy { it.second }
        relativeFreqList.reverse()
        // cutoff for top 100 results
        if (relativeFreqList.size > maxKWs) {
            relativeFreqList = relativeFreqList.slice(0..(maxKWs - 1)).toMutableList()
        }
        Log.d("litdao", relativeFreqList.toString())
        var result: MutableList<Pair<String, Double>> = mutableListOf<Pair<String, Double>>()
        for (res in relativeFreqList) {
            result.add(Pair(res.first, res.second))
        }
        return result
    }

    fun preInitializedExtract(
        text: String,
        csv: Map<String, Long>
    ): MutableList<Pair<String, Double>> {

        // Establish general averages
        val avgTotal = 588089694315
        val avgFreqMap = csv

        // Append a word to the input as the used library forgets about the last word
        // Stem the input using the snowball library and split into list of words
        var input = stem(text.plus(" bugFix"))
            .split("\\s".toRegex())

        // Instantiate a map for the word counts and a counter for the total words that were recognized
        var freqMap = mutableMapOf<String, Int>()
        var total = 0

        // Count words known in averageFreqMap in input
        for (word in input) {
            val avgFreq = avgFreqMap[word]
            if (avgFreq == null) continue
            total += 1
            if (word !in freqMap.keys) {
                freqMap[word] = 1
            } else {
                freqMap[word] = freqMap.getValue(word) + 1
            }
        }

        // Instantiate a result list
        var relativeFreqList = mutableListOf<Result>()

        //Calculate relative frequencies per word
        for ((word, freq) in freqMap.entries) {
            var avgFreq = avgFreqMap[word]
            if (avgFreq == null) {
                avgFreq = 1
            }
            val relativeFreq =
                (freq.toDouble() / total.toDouble()) / (avgFreq.toDouble() / avgTotal.toDouble())
            // filter for relevance of the word
            if (relativeFreq > hardCodedMagickNumber) {
                relativeFreqList.add(Result(word, relativeFreq))
            }
        }
        // sort results (not working yet)
        relativeFreqList.sortBy { it.second }
        relativeFreqList.reverse()
        // cutoff for top 100 results
        if (relativeFreqList.size > maxKWs) {
            relativeFreqList = relativeFreqList.slice(0..(maxKWs - 1)).toMutableList()
        }
        Log.d("litdao", relativeFreqList.toString())
        var result: MutableList<Pair<String, Double>> = mutableListOf<Pair<String, Double>>()
        for (res in relativeFreqList) {
            result.add(Pair(res.first, res.second))
        }
        return result
    }
}
