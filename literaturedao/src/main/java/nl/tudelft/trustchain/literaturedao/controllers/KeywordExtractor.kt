
package nl.tudelft.trustchain.literaturedao.controllers

import android.R
import android.util.Log
import nl.tudelft.trustchain.literaturedao.LiteratureDaoActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import nl.tudelft.trustchain.literaturedao.snowball.Main.main as stem


class KeywordExtractor : LiteratureDaoActivity() {

    // Function that loads the acerage stemmed word occurance
    /*
    fun instantiateAvgFreqMap(): Map<String, Long>{

        var res = mutableMapOf<String, Long>()
        //android.content.Context.getAssets()
        Log.d("litdao", "1")
        val asset = assets.open("stemmed_freqs.csv")
        //val is = androidContext()
        Log.d("litdao", "2")
        val reader = BufferedReader(InputStreamReader(asset))
        Log.d("litdao", "3")

        var line : String
        while (reader.readLine().also { line = it } != null){
            val key = line.split(",".toRegex())[0]
            val num = line.split(",".toRegex())[1].toLong()
            res[key] = num
        }



        return res
    }*/

    // Custom data type in order to be able to sort
    class Result constructor(word: String, relativeFreq: Double) {
        val first = word
        val second = relativeFreq
        fun second(): Double {
            return second
        }
        override fun toString(): String{
            val builder = StringBuilder()
            builder.append("(")
                .append(first)
                .append(", ")
                .append(second.toString())
                .append(")")
            return  builder.toString()
        }
    }

    // Quik fix alternative if the actual implementation does not work
    fun quikFix(arg: String): kotlin.collections.MutableList<Result> {

        //List of frequently used words
        val banList = listOf("a", "about", "above", "actually", "after", "again", "against", "all", "almost", "also", "although", "always", "am", "an", "and", "any", "are", "as", "at", "be", "became", "become", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can", "could", "did", "do", "does", "doing", "down", "during", "each", "either", "else", "few", "for", "from", "further", "had", "has", "have", "having", "he", "hence", "her", "here", "hers", "herself", "him", "himself", "his", "how", "I", "if", "in", "into", "is", "it", "its", "itself", "just", "may", "maybe", "me", "might", "mine", "more", "most", "must", "my", "myself", "neither", "nor", "not", "of", "oh", "on", "once", "only", "ok", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "she", "should", "so", "some", "such", "than", "that", "the", "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "we", "were", "what", "when", "whenever", "where", "whereas", "wherever", "whether", "which", "while", "who", "whoever", "whose", "whom", "why", "will", "with", "within", "would", "yes", "yet", "you", "your", "yours", "yourself", "yourselves")

        var input = stem(arg)
            .split("\\s".toRegex())

        var freqMap = mutableMapOf<String, Int>()

        var total = 0

        // Count words
        for (word in input){
            if (word in banList) continue
            total += 1
            if (word !in freqMap.keys){
                freqMap[word] = 1
            } else {
                freqMap[word] = freqMap.getValue(word) + 1
            }
        }

        var pressenceList = mutableListOf<Result>()

        //Calculate word pressence
        for ((word, freq) in freqMap.entries){
            pressenceList.add(Result(word, freq.toDouble() / total.toDouble()))
        }

        pressenceList.sortBy { it.second }
        pressenceList.reverse()
        return pressenceList
    }

    /*fun actualImplementation(text: String): kotlin.collections.MutableList<Result> {

        // Establish general averages
        val avgTotal = 588089694315
        val avgFreqMap = instantiateAvgFreqMap()

        // Append a word to the input as the used library forgets about the last word
        // Stem the input using the snowball library and split into list of words
        var input = stem(text.plus(" bugFix"))
            .split("\\s".toRegex())

        // Instantiate a map for the word counts and a counter for the total words that were recognized
        var freqMap = mutableMapOf<String, Int>()
        var total = 0

        // Count words known in averageFreqMap in input
        for (word in input){
            val avgFreq = avgFreqMap[word]
            if (avgFreq == null) continue
            total += 1
            if (word !in freqMap.keys){
                freqMap[word] = 1
            } else {
                freqMap[word] = freqMap.getValue(word) + 1
            }
        }

        // Instantiate a result list
        var relativeFreqList = mutableListOf<Result>()

        //Calculate relative frequencies per word
        for ((word, freq) in freqMap.entries){
            val avgFreq = avgFreqMap[word]
            if (avgFreq == null) continue
            relativeFreqList.add(Result(word, (freq.toDouble() / total.toDouble())  / (avgFreq.toDouble() / avgTotal.toDouble())))
        }

        //sort results (not working yet)
        relativeFreqList.sortBy { it.second }
        relativeFreqList.reverse()
        Log.d("litdao", relativeFreqList.toString())
        return relativeFreqList
    }*/
}
