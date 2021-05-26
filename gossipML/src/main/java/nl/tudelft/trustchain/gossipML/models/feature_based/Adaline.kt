package nl.tudelft.trustchain.gossipML.models.feature_based
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.gossipML.models.OnlineModel
import java.util.*

/**
 * Machine Learning Adaline model (Adaptive Linear Neuron)
 * Single layer NN used in gossiping user preference models
 */
@Serializable
class Adaline : OnlineModel {
    private val learningRate: Double
    var bias = Random().nextDouble()

    constructor(learningRate: Double, amountFeatures: Int) : super(amountFeatures, "Adaline") {
        this.learningRate = learningRate
    }

    /**
     * update model with single data instance
     *
     * @param x - feature array
     * @param y - label
     */
    override fun update(x: Array<Double>, y: Double) {
        val error = y - activation(forward(x))
        this.bias += this.learningRate * error
        for ((idx, item) in x.withIndex()) {
            weights[idx] = weights[idx] + learningRate * error * item
        }
    }

    /**
     * update model with multiple data instances
     *
     * @param x - array of feature arrays
     * @param y - array of labels
     */
    override fun update(x: Array<Array<Double>>, y: Array<Double>) {
        require(x.size == y.size) {
            String.format("Input vector x of size %d not equal to length %d of y", x.size, y.size)
        }
        for (i in x.indices) {
            update(x[i], y[i])
        }
    }

    /**
     * predict score for a given data instance
     *
     * @param x feature array
     * @return predicted score
     */
    override fun predict(x: Array<Double>): Double {
        return activation(forward(x))
    }

    /**
     * binary classification of input instance based on activation
     * e.g. - like / dislike song
     *
     * @param x feature array
     * @return binary predicted label
     */
    fun classify(x: Array<Double>): Double {
        return if (activation(forward(x)) >= 0.0) {
            1.0
        } else {
            0.0
        }
    }

    /**
     * Adaline activation function
     *
     * @param x output of the previous neuron
     * @return activation of x
     */
    private fun activation(x: Double): Double {
        return x
    }

    /**
     * Forward pass of Adaline
     *
     * @param x single data instance
     * @return propagated value
     */
    private fun forward(x: Array<Double>): Double {
        var weightedSum = this.bias
        for (idx in 1 until x.size) {
            weightedSum += this.weights[idx] * x[idx]
        }
        return weightedSum
    }

    override fun serialize(): String {
        return Json.encodeToString(this)
    }
}
