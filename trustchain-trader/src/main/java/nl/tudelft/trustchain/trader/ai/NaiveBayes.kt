package nl.tudelft.trustchain.trader.ai

import com.opencsv.CSVIterator
import com.opencsv.CSVReader
import toNaiveBayesClassifier
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Training point for the classifier
 */
class PricePoint(val exchangeRate: Int, val tradeType: Int)

/**
 * Class for interacting with the naive bayes classifier
 */
class NaiveBayes(trainingStream: InputStream) {
    private val pricePointList = mutableListOf<PricePoint>()

    init {
        val reader = CSVReader(InputStreamReader(trainingStream))
        val iterator = CSVIterator(reader).iterator()
        iterator.next()
        while (iterator.hasNext()) {
            val line = iterator.next()
            val exchangeRate = (line[0].toDouble() * 100).toInt()
            val tradeType = line[1].toInt()
            pricePointList.add(
                PricePoint(
                    exchangeRate,
                    tradeType
                )
            )
        }
    }

    fun pricePointToFeatureSet(pricePoint: PricePoint): Set<Int> {
        return setOf(pricePoint.exchangeRate)
    }

    val nbc = pricePointList.toNaiveBayesClassifier(
        featuresSelector = { pricePointToFeatureSet(it) },
        categorySelector = { it.tradeType }
    )

    fun predict(exchangeRate: Int): Int {
        val prediction = nbc.predict(exchangeRate)
        if (prediction != null) {
            return prediction
        } else {
            return 2
        }
    }
}
