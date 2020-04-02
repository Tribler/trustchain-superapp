package nl.tudelft.trustchain.trader.ai

import android.util.Log
import com.opencsv.CSVIterator
import com.opencsv.CSVReader
import toNaiveBayesClassifier
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Training point for the classifier
 */
class PricePoint(val exchangeRate: Int, val type: Int, val act: Int)

/**
 * Class for interacting with the naive bayes classifier
 */
class NaiveBayes(private val trainingStream: InputStream) {

    /**
     * Import a csv file with training points to train the model
     */
    private fun importCSVToPricePoints(): List<PricePoint> {
        val reader = CSVReader(InputStreamReader(trainingStream))
        val pricePointList = mutableListOf<PricePoint>()
        val iterator = CSVIterator(reader).iterator()
        iterator.next()
        var i = 0
        while (iterator.hasNext()) {
            val line = iterator.next()
            val exchangeRate = line[0].toInt()
            val tradeType = line[1].toInt()
            val act = line[2].toInt()
            pricePointList.add(
                PricePoint(
                    exchangeRate,
                    tradeType,
                    act
                )
            )
        }
        return pricePointList
    }

    /**
     * Turns the PricePoint data into a feature set
     */
    fun pricePointToFeatureSet(pricePoint: PricePoint): Set<Int> {
        return setOf(pricePoint.exchangeRate, pricePoint.type.toInt())
    }

    /**
     * Train the classifier
     */
    val prices = importCSVToPricePoints()
    val nbc = prices.toNaiveBayesClassifier(
        featuresSelector = { pricePointToFeatureSet(it) },
        categorySelector = { it.act }
    )

    /**
     * Predict the action, given these features.
     */
    fun predict(price: Int, type: Int): Int {
        val prediction = nbc.predict(price, type)
        if (prediction != null) return prediction
        return 0
    }

    /**
     * Test the model.
     */
    fun test() {
        Log.d("Ask(1) placed for $50, prediction result: ", nbc.predictWithProbability(50,1).toString())
        Log.d("Ask(1) placed for $60, prediction result: ", nbc.predictWithProbability(60,1).toString())
        Log.d("Ask(1) placed for $70, prediction result: ", nbc.predictWithProbability(70,1).toString())
        Log.d("Ask(1) placed for $80, prediction result: ", nbc.predictWithProbability(80,1).toString())
        Log.d("Ask(1) placed for $90, prediction result: ", nbc.predictWithProbability(90,1).toString())
        Log.d("Ask(1) placed for $95, prediction result: ", nbc.predictWithProbability(95,1).toString())

        Log.d("INFO", "---Approaching the mean of $100 per BTC---")
        Log.d("Ask(1) placed for $100, prediction result: ", nbc.predictWithProbability(100,1).toString())
        Log.d("Bid(0) placed for $100, prediction result: ", nbc.predictWithProbability(100,0).toString())
        Log.d("INFO","---Departing the mean of $100 per BTC---")

        Log.d("Bid(0) placed for $105, prediction result: ", nbc.predictWithProbability(105,0).toString())
        Log.d("Bid(0) placed for $110, prediction result: ", nbc.predictWithProbability(110,0).toString())
        Log.d("Bid(0) placed for $120, prediction result: ", nbc.predictWithProbability(120,0).toString())
        Log.d("Bid(0) placed for $130, prediction result: ", nbc.predictWithProbability(130,0).toString())
        Log.d("Bid(0) placed for $140, prediction result: ", nbc.predictWithProbability(140,0).toString())
        Log.d("Bid(0) placed for $150, prediction result: ", nbc.predictWithProbability(150,0).toString())
    }
}
