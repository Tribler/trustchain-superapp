package nl.tudelft.trustchain.literaturedao.controllers

import android.util.Log
import nl.tudelft.trustchain.literaturedao.LiteratureDaoActivity
import nl.tudelft.trustchain.literaturedao.data_types.Literature
import nl.tudelft.trustchain.literaturedao.snowball.Main.main as stem


class QueryHandler : LiteratureDaoActivity() {

    // Convert an input string into a handleable query
    fun toQuery(input: String): List<String> {
        val inp = input + " bugfix"
        val stemmed = stem(inp)
        val regexed = stemmed.split("\\s".toRegex())
        return regexed

    }

    // Score a pdf for given query
    fun score(query: List<String>, keyWords: List<Pair<String, Double>>): Double {
        var score: Double = 0.0
        var matches = 0
        for (word in query) {
            for (pair in keyWords) {
                if (word == pair.first) {
                    score += pair.second
                    matches += 1
                }
            }
        }
        if (matches == 0) {
            return 0.0
        }
        score /= matches
        return score
    }

    // Custom data type in order to be able to sort
    class Result constructor(identifier: Literature, score: Double) {
        val first = identifier
        val second = score
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

    // Return a sorted list with the scores of local pdf's for a given query
    fun scoreList(
        inp: String,
        content: MutableList<Literature>
    ): MutableList<Pair<Literature, Double>> {

        val query = toQuery(inp)
        var scoreList: MutableList<Result> = mutableListOf<Result>()
        Log.e("litdao", "scoreing")
        Log.e("litdao", "Content: " + content.toString())
        for (lit in content) {
            Log.e("litdao", "A piece of lit scored: " + lit.toString())
            val score: Double = score(query, lit.keywords)
            val identifier: Literature = lit;
            scoreList.add(Result(identifier, score))
        }
        scoreList.sortBy { it.second }
        scoreList.reverse()
        var result: MutableList<Pair<Literature, Double>> =
            mutableListOf<Pair<Literature, Double>>()
        for (res in scoreList) {
            result.add(Pair(res.first, res.second))
        }
        return result
    }
}
