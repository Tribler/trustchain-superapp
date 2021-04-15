package nl.tudelft.trustchain.gossipML.models.collaborative_filtering

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.gossipML.models.Model
import nl.tudelft.trustchain.gossipML.models.plus
import nl.tudelft.trustchain.gossipML.models.times
import java.lang.Double.NEGATIVE_INFINITY
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random.Default.nextDouble

object SongFeatureSerializer : KSerializer<SortedMap<String, SongFeature>> {
    private val mapSerializer = MapSerializer(String.serializer(), SongFeature.serializer())
    override val descriptor: SerialDescriptor = mapSerializer.descriptor
    override fun serialize(encoder: Encoder, value: SortedMap<String, SongFeature>) {
        mapSerializer.serialize(encoder, value)
    }
    override fun deserialize(decoder: Decoder): SortedMap<String, SongFeature> {
        return mapSerializer.deserialize(decoder).toSortedMap()
    }
}

object RatingSerializer : KSerializer<SortedMap<String, Double>> {
    private val mapSerializer = MapSerializer(String.serializer(), Double.serializer())
    override val descriptor: SerialDescriptor = mapSerializer.descriptor
    override fun serialize(encoder: Encoder, value: SortedMap<String, Double>) {
        mapSerializer.serialize(encoder, value)
    }
    override fun deserialize(decoder: Decoder): SortedMap<String, Double> {
        return mapSerializer.deserialize(decoder).toSortedMap()
    }
}

/**
 * Publicly avaliable part of matrix factarization model
 *
 * @property peerFeatures amount of peer features
 */
@Serializable
data class PublicMatrixFactorization(
    @Serializable(with = SongFeatureSerializer::class)
    val peerFeatures: SortedMap<String, SongFeature>
) : Model("PublicMatrixFactorization")

/**
 * Class that represents song features for matrix factorization model
 *
 * @property age song age
 * @property feature normal song features
 * @property bias song bias
 */
@Serializable
data class SongFeature(
    var age: Double,
    var feature: Array<Double>,
    var bias: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SongFeature
        if (age != other.age) return false
        if (!feature.contentEquals(other.feature)) return false
        if (bias != other.bias) return false
        return true
    }

    override fun hashCode(): Int {
        var result = age.hashCode()
        result = 31 * result + feature.contentHashCode()
        result = 31 * result + bias.hashCode()
        return result
    }
}

fun songFeaturesFromArrays(age: Array<Double>, features: Array<Array<Double>>, bias: Array<Double>): Array<SongFeature> {
    return age.zip(features).zip(bias) { (a, b), c -> SongFeature(a, b, c) }.toTypedArray()
}

/**
 * General matrix factorization model
 *
 * @property ratings
 */
@Serializable
open class MatrixFactorization(
    @Serializable(with = RatingSerializer::class) var ratings: SortedMap<String, Double>
) : Model("MatrixFactorization") {
    val k = 5

    private val lr = 0.01
    private val lambda = 1

    private val minR = 0.0
    private var maxR = 20.0

    @Serializable(with = SongFeatureSerializer::class)
    var songFeatures = ratings.keys.zip(
        songFeaturesFromArrays(
            Array(ratings.size) { i -> if (ratings[ratings.keys.toTypedArray()[i]]!! > 0.0) 1.0 else 0.0 }, // ages
            Array(ratings.size) { Array(k) { initFeat() } }, // song features
            Array(ratings.size) { 0.0 } // biases
        )
    ).toMap().toSortedMap()

    private var rateFeature = Array(k) { initFeat() }
    private var rateBias = 0.0

    constructor(peerModel: PublicMatrixFactorization) : this(peerModel.peerFeatures)
    constructor(peerFeatures: SortedMap<String, SongFeature>, True: Boolean = true) : this(
        ratings = peerFeatures.keys.zip(Array(peerFeatures.size) { 0.0 }).toMap().toSortedMap()
    ) {
        // True in constructor to avoid java type signature class with main constructor
        if (True) this.songFeatures = peerFeatures.toSortedMap()
    }

    /**
     * Initalize feature
     *
     * @return initialized feature
     */
    private fun initFeat(): Double {
        return nextDouble() * sqrt((maxR - minR) / k)
    }

    /**
     * Predict most fitting song
     *
     * @return name of most relevant song
     */
    fun predict(): String {
        var bestSong = ""
        var mostRelevant = NEGATIVE_INFINITY
        songFeatures.forEach {
            val (name, triple) = it
            val (_, feature, bias) = triple
            if (ratings[name]!! == 0.0) {
                val relevance = rateFeature * feature + rateBias + bias
                if (relevance > mostRelevant) {
                    bestSong = name
                    mostRelevant = relevance
                }
            }
        }
        Log.i("Recommend", "Best colaborative score: $mostRelevant")
        return bestSong
    }

    /**
     * Updates matrix ratings
     *
     * @param newRatings new ratings to be merged with old ones
     */
    fun updateRatings(newRatings: SortedMap<String, Double>) {
        for ((name, rating) in newRatings) {
            ratings[name] = rating
            if (songFeatures[name] == null) { // maybe initialize newly rated song
                songFeatures[name] = SongFeature(0.0, Array(k) { initFeat() }, 0.0)
            }
        }
        update()
    }

    /**
     * Update helper method to update songs features
     *
     */
    override fun update() {
        var errTotal = 0.0
        songFeatures.forEach {
            val (name, triple) = it
            val (age, feature, bias) = triple
            if (ratings[name] != 0.0) {
                val err = ratings[name]!! - rateFeature * feature - rateBias - bias
                errTotal += abs(err)

                val (newSongFeature, newRateFeature) = Pair(
                    (1.0 - lr * lambda) * feature + lr * err * rateFeature,
                    (1.0 - lr * lambda) * rateFeature + lr * err * feature
                )

                songFeatures[name]!!.age = age + 1.0

                songFeatures[name]!!.feature = newSongFeature
                rateFeature = newRateFeature

                songFeatures[name]!!.bias += lr * err
                rateBias += lr * err
            }
        }
    }

    /**
     * Open function to merge matrix fatorization models
     *
     * @param peerModel
     */
    open fun merge(peerModel: PublicMatrixFactorization) {
        merge(peerModel.peerFeatures)
    }

    /**
     * Actual merging function
     *
     * @param peerFeatures features to be merged with
     */
    open fun merge(peerFeatures: SortedMap<String, SongFeature>) {
        if (peerFeatures.keys.toSet() != songFeatures.keys) {
            for (name in peerFeatures.keys.toSet() + songFeatures.keys.toSet()) {
                // initialize rows not yet present in each map
                if (songFeatures[name] == null) {
                    songFeatures[name] = SongFeature(0.0, Array(k) { initFeat() }, 0.0)
                }
                if (ratings[name] == null) {
                    ratings[name] = 0.0
                }
                if (peerFeatures[name] == null) {
                    peerFeatures[name] = SongFeature(0.0, Array(k) { initFeat() }, 0.0)
                }
            }
        }

        // age weighted average, more weight to rows which have been updated many times
        songFeatures.forEach {
            val (name, triple) = it
            val (age, feature, bias) = triple
            val tripleNew = peerFeatures[name]!!
            val (ageNew, featureNew, biasNew) = tripleNew
            if (ageNew != 0.0) {
                val w = ageNew / (age + ageNew)
                songFeatures[name] = SongFeature(
                    age = max(age, ageNew),
                    feature = feature * (1 - w) + featureNew * w,
                    bias = bias * (1 - w) + biasNew * w
                )
            }
        }

        update()
    }

    private fun compress(map: SortedMap<String, SongFeature>): SortedMap<String, SongFeature> {
        /*
         from paper: subsample songFeatures to a fixed number of rows instead of whole thing
         maybe also compress with lz4 or something?
         */
        return map
    }

    /**
     * the function used during automatic serialization in model exchange messages
     * don't serialize private ratings matrix
     */
    override fun serialize(): String {
        return this.serialize(private = false)
    }

    /**
     * the function used during serialization for local database storage
     */
    fun serialize(private: Boolean): String {
        Log.i(
            "Recommend",
            "Serializing MatrixFactorization, including private data: $private"
        )
        return if (private) Json.encodeToString(this)
        else Json.encodeToString(PublicMatrixFactorization(compress(songFeatures.toSortedMap())))
    }
}
