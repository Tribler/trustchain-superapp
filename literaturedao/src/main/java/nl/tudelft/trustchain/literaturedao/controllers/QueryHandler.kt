
package nl.tudelft.trustchain.literaturedao.controllers

import android.util.Log
import nl.tudelft.trustchain.literaturedao.LiteratureDaoActivity
import nl.tudelft.trustchain.literaturedao.snowball.Main.main as stem


class QueryHandler : LiteratureDaoActivity() {

    // Convert an input string into a handleable query
    fun toQuery(inp: String): List<String>{
        return stem(inp)
            .split("\\s".toRegex())
    }

    // Score a pdf for given query
    fun score(query: List<String>, keyWords: List<Pair<String, Double>>): Double{
        var score: Double = 0.0
        var matches = 0
        for (word in query){
            for (pair in keyWords){
                if (word == pair.first){
                    score += pair.second
                    matches += 1
                }
            }
        }
        if( matches == 0){
            return 0.0
        }
        score /= matches
        return score
    }

    // Custom data type in order to be able to sort
    class Result constructor(identifier: String, score: Double) {
        val first = identifier
        val second = score
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

    // Return a sorted list with the scores of local pdf's for a given query
    fun scoreList(inp: String, allTheKWLists: List<Pair<String, List<Pair<String, Double>>>>): MutableList<Pair<String, Double>>{
        val query = toQuery(inp)
        var scoreList: MutableList<Result> = mutableListOf<Result>()
        for (KWList in allTheKWLists){
            val score: Double = score(query, KWList.second)
            val identifier: String = KWList.first
            scoreList.add(Result(identifier, score))
        }
        scoreList.sortBy { it.second }
        scoreList.reverse()
        var result: MutableList<Pair<String, Double>> = mutableListOf<Pair<String, Double>>()
        for(res in scoreList){
            result.add(Pair(res.first, res.second))
        }
        return result
    }
}
