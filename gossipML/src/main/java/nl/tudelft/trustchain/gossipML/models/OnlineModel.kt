package nl.tudelft.trustchain.gossipML.models
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.util.*

/**
 * Online model generalization of Adaline and Pegasos models (feature-based models)
 */
@Serializable
open class OnlineModel : Model {
    val amountFeatures: Int
    var weights: Array<Double>

    constructor(amountFeatures: Int, name: String) : super(name) {
        this.amountFeatures = amountFeatures
        this.weights = Array(amountFeatures) { _ -> 1.0 }
    }

    /**
     * merge 2 online models
     *
     * @param otherOnlineModel other model to merge with
     * @return merged model
     */
    fun merge(otherOnlineModel: OnlineModel): OnlineModel {
        for (idx in weights.indices) {
            weights[idx] = (weights[idx] * 0.9) + (otherOnlineModel.weights[idx] / 10) // it was divied by 2?
        }
        return this
    }

    /**
     * make predictions for a batch of input instances
     *
     * @param x - array of feature arrays
     * @param y - array of labels
     * @return array of predictions
     */
    fun predict(x: Array<Array<Double>>): Array<Double> {
        val result = Array(x.size) { 0.0 }
        for ((idx, item) in x.withIndex()) {
            result[idx] = predict(item)
        }
        return result
    }

    /**
     * test method for getting prediction accuracy on some test instances
     *
     * @param x - array of feature arrays
     * @param y - array of labels
     * @return prediction score
     */
    fun score(x: Array<Array<Double>>, y: Array<Double>): Double {
        var correct = 0.0
        for (i in x.indices) if (predict(x[i]) == y[i]) correct ++
        return (correct / x.size)
    }

    open fun update(x: Array<Array<Double>>, y: Array<Double>) {}

    open fun predict(x: Array<Double>): Double {
        return 1.0
    }

    open fun update(x: Array<Double>, y: Double) {}

    override fun serialize(): String {
        return Json.encodeToString(this)
    }
}
