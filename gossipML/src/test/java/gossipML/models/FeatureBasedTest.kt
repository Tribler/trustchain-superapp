package nl.tudelft.trustchain.gossipML.models

import nl.tudelft.trustchain.gossipML.db.scale2label
import nl.tudelft.trustchain.gossipML.db.stats
import nl.tudelft.trustchain.gossipML.models.feature_based.Adaline
import nl.tudelft.trustchain.gossipML.models.feature_based.Pegasos
import org.hamcrest.CoreMatchers.instanceOf
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class FeatureBasedTest {
    private val amountFeatures = 10
    private var features: Array<Array<Double>> = Array(amountFeatures) { _ -> Array<Double>(amountFeatures) { Random.nextDouble(0.0, 5.0) } }
    private var labels = Array(amountFeatures) { Random.nextInt(0, 2).toDouble() }
    private var highVarianceFeatureLabels = arrayOf(3, 62, 162, 223, 130, 140)

    @Test
    fun testPegasos() {
        val model = Pegasos(0.4, amountFeatures, 5)

        model.update(features, labels)

        model.predict(Array(amountFeatures) { Random.nextDouble(0.0, 5.0) })

        val mergeModelEq = Pegasos(0.4, amountFeatures, 5)
        model.merge(mergeModelEq)
        Assert.assertThat(model, instanceOf(Pegasos::class.java))

        val mergeModelDiff = Adaline(0.1, amountFeatures)
        model.merge(mergeModelDiff)
        Assert.assertThat(model, instanceOf(Pegasos::class.java))
    }

    @Test
    fun testAdaline() {
        val model = Adaline(0.1, amountFeatures)

        model.update(features, labels)

        model.predict(Array(amountFeatures) { Random.nextDouble(0.0, 5.0) })

        val mergeModelEq = Adaline(0.1, amountFeatures)
        model.merge(mergeModelEq)
        Assert.assertThat(model, instanceOf(Adaline::class.java))

        val mergeModelDiff = Pegasos(0.4, amountFeatures, 5)
        model.merge(mergeModelDiff)
        Assert.assertThat(model, instanceOf(Adaline::class.java))
    }

    @Test
    fun testAdalinePredictions() {
        val model = Adaline(1.0, 2)
        val biasedFeatures = arrayOf(arrayOf(100.0), arrayOf(-1.0))
        val biasedLabels = arrayOf(50.0, 0.0)

        for (i in 0..100) {
            model.update(biasedFeatures, biasedLabels)
        }

        val biasedTestSamples = arrayOf(arrayOf(100.0), arrayOf(-1.0))

        val res = model.predict(biasedTestSamples)
        Assert.assertTrue(res[0] >= res[1])
    }

    private fun pairwiseDifference(models: Array<OnlineModel>): Double {
        var diff = 0.0
        var total = 0.0
        for (m1 in models) {
            for (m2 in models) {
                if (m1 === m2) continue
                for (k in m1.weights.indices) {
                    diff += (m1.weights[k] - m2.weights[k]).absoluteValue
                    total += (m1.weights[k] + m2.weights[k]) / 2
                }
            }
        }
        return diff / total
    }

    @Test
    fun testGossipConvergence() {
        val model1 = Adaline(1.0, 4)
        val model11 = Adaline(1.0, 4)

        val model2 = Pegasos(0.1, 4, 100)
        val model22 = Pegasos(0.1, 4, 100)

        for (i in 0..100) {
            model1.merge(model11)
            model2.merge(model22)

            model11.merge(model1)
            model22.merge(model2)
        }

        val models1 = arrayOf(model1 as OnlineModel, model11 as OnlineModel)
        val models2 = arrayOf(model2 as OnlineModel, model22 as OnlineModel)

        Assert.assertTrue(pairwiseDifference(models1) < 0.1)
        Assert.assertTrue(pairwiseDifference(models2) < 0.1)
    }

    @Test
    fun testToyRecommendations() {
        val features = arrayOf(
            arrayOf(-1.0, 97.0, -1.0, -1.0),
            arrayOf(-1.0, 101.0, -1.0, -1.0),
            arrayOf(-1.0, -1.0, -1.0, -1.0),
            arrayOf(-1.0, 95.0, -1.0, -1.0),
            arrayOf(-1.0, 97.0, -1.0, -1.0),
            arrayOf(-1.0, 101.0, -1.0, -1.0),
            arrayOf(-1.0, 1.0, -1.0, -1.0),
            arrayOf(-1.0, 95.0, -1.0, -1.0),
            arrayOf(-1.0, 103.0, -1.0, -1.0),
            arrayOf(-1.0, 101.0, -1.0, -1.0),
            arrayOf(-1.0, 0.0, -1.0, -1.0),
            arrayOf(-1.0, 96.0, -1.0, -1.0),
        )
        val labels = arrayOf(50.0, 44.0, 0.0, 49.0, 50.0, 44.0, 0.0, 49.0, 50.0, 44.0, 0.0, 49.0)
        val model = Pegasos(0.1, 4, 100)
        model.update(features, labels)

        var correctPredictions = 0

        for (i in 0..10) {
            val test = arrayOf(
                arrayOf(-1.0, Random.nextDouble(95.0, 100.0), -1.0, -1.0),
                arrayOf(-1.0, Random.nextDouble(95.0, 100.0), -1.0, -1.0),
                arrayOf(-1.0, Random.nextDouble(-1.0, 1.0), -1.0, -1.0),
                arrayOf(-1.0, Random.nextDouble(95.0, 100.0), -1.0, -1.0),
            ).map { model.predict(it) }

            if (test[0] >= test[2] && test[1] >= test[2] && test[3] >= test[2]) {
                correctPredictions += 1
            }
        }

        Assert.assertTrue(correctPredictions >= 5)
    }

    /**
     * Use this to get statistics for local training data
     */
    fun calculateStats(classRanges: Array<Int>, features: Array<Array<Double>>) {
        val means = Array<Array<Double>>(5) { emptyArray() }
        val variance = Array<Array<Double>>(5) { emptyArray() }
        for (i in classRanges.indices) {
            var counter = 0
            if (i > 0) {
                means[i - 1] = Array<Double>(225) { 0.0 }
                variance[i - 1] = Array<Double>(225) { 0.0 }
                val subf = features.copyOfRange(classRanges[i - 1], classRanges[i])
                for (fs in subf) {
                    means[i - 1] += fs
                    counter += 1
                }
                means[i - 1] = means[i - 1].map { p -> p / counter }.toTypedArray()

                for (fs in subf) {
                    variance[i - 1] += (fs - means[i - 1]).map { p -> p.pow(2) }.toTypedArray()
                }

                variance[i - 1] = variance[i - 1].map { p -> sqrt(p / counter) }.toTypedArray()
            }
        }
    }

    @Test
    fun testRecommendations() {
        val numFeat = highVarianceFeatureLabels.size
        val classRanges = Array(6) { -1 }
        val mutFeatures: MutableList<Array<Double>> = mutableListOf()
        var c = 0
        var f = 0
        File("src/test/res").listFiles()!!.forEach { classDir ->
            classRanges[c] = f
            classDir.listFiles()!!.forEach { jsonFile ->
                mutFeatures.add(featuresFromJson(jsonFile))
                f += 1
            }
            c += 1
        }

        classRanges[c] = f

        val features = mutFeatures.toTypedArray()

        val usersPerGroup = 3
        val models = Array(usersPerGroup * 5) { Pegasos(0.1, numFeat, 100) }
        val globalModel = Pegasos(0.1, numFeat, 100)

        val plays: MutableList<Array<Double>> = mutableListOf()
        for ((i, _) in models.withIndex()) {
            val myClass = i / usersPerGroup
            // 10 random songs in-class
            val likedIdxs = Array(10) { (classRanges[myClass]..classRanges[myClass + 1]).random() }
            val likedPlays = Array(10) { (20..50).random().toDouble() }

            // 7 random songs out-of-class
            val mehIdxs = Array(7) {
                var idx = (0..features.size).random()
                while (classRanges[myClass] <= idx && idx < classRanges[myClass + 1]) idx = (0..features.size).random()
                idx
            }
            val mehPlays = Array(7) { (0..1).random().toDouble() }

            val playMap = arrayOf(*likedIdxs, *mehIdxs).zip(arrayOf(*likedPlays, *mehPlays)).toMap()
            val play = features.mapIndexed { fid, _ -> playMap[fid] ?: -1.0 }.toTypedArray()
            plays.add(play)
        }

        // gossip train for global model
        repeat(100) {
            for ((i, mi) in models.withIndex()) {
                for ((j, mj) in models.withIndex()) {
                    val flag = Random.nextBoolean()
                    if (flag) {
                        mj.merge(globalModel)
                        globalModel.update(features, plays[j])
                    } else {
                        mi.merge(globalModel)
                        globalModel.update(features, plays[i])
                    }

                    // we bias our model by re-training it every time on local features
                    for ((k, mk) in models.withIndex()) {
                        mk.update(features, plays[k])
                        mk.update(features, plays[k])
                    }
                }
            }
        }

        val correct = models.mapIndexed { i, m ->
            val myClass = i / 5
            val preds = m.predict(features)
            val predIdx = preds.indexOfFirst { it == preds.maxOrNull()!! }
            if (classRanges[myClass] <= predIdx && predIdx < classRanges[myClass + 1]) 1 else 0
        }
        val numCorrect = correct.sum()
        Assert.assertTrue("num correct: $numCorrect", numCorrect >= 0)
    }

    fun featuresFromJson(jsonFile: File): Array<Double> {
        val year = -1.0
        val genre = -1.0

        val essentiaFeatures = JSONObject(jsonFile.bufferedReader().use { it.readText() })

        val lowlevel = essentiaFeatures.getJSONObject("lowlevel")

        val dynamic_complexity = lowlevel.getDouble("dynamic_complexity")
        val average_loudness = lowlevel.getDouble("average_loudness")

        var integrated_loudness = -1.0
        var loudness_range = -1.0
        var momentary = Array(5) { -1.0 }
        var short_term = Array(5) { -1.0 }
        try {
            integrated_loudness = lowlevel.getJSONObject("loudness_ebu128").getDouble("integrated")
            loudness_range = lowlevel.getJSONObject("loudness_ebu128").getDouble("loudness_range")
            momentary = stats(lowlevel.getJSONObject("loudness_ebu128").getJSONObject("momentary"))
            short_term = stats(lowlevel.getJSONObject("loudness_ebu128").getJSONObject("short_term"))
        } catch (e: Exception) {
        }

        val keys = arrayOf(
            "barkbands_crest", "barkbands_flatness_db", "barkbands_kurtosis", "barkbands_skewness", "barkbands_spread",
            "dissonance", "erbbands_crest", "erbbands_flatness_db", "erbbands_kurtosis", "erbbands_skewness",
            "erbbands_spread", "hfc", "melbands_crest", "melbands_flatness_db", "melbands_kurtosis", "melbands_skewness",
            "melbands_spread", "pitch_salience", "spectral_centroid", "spectral_complexity", "spectral_decrease",
            "spectral_energy", "spectral_energyband_high", "spectral_energyband_low", "spectral_energyband_middle_high",
            "spectral_energyband_middle_low", "spectral_entropy", "spectral_flux", "spectral_kurtosis", "spectral_rms",
            "spectral_rolloff", "spectral_skewness", "spectral_spread", "spectral_strongpeak", "zerocrossingrate"
        )
        val lowlevelStats = Array(keys.size) { Array(5) { -1.0 } }
        for ((i, key) in keys.withIndex()) {
            lowlevelStats[i] = stats(lowlevel.getJSONObject(key))
        }

        val tonal = essentiaFeatures.getJSONObject("tonal")

        val key = scale2label(tonal.getString("chords_key"), tonal.getString("chords_scale"))

        var key_edma: Array<Double>
        var key_krumhansl = arrayOf(0.0, 0.0)
        var key_temperley = arrayOf(0.0, 0.0)
        try {
            key_edma = scale2label(
                tonal.getJSONObject("key_edma").getString("key"),
                tonal.getJSONObject("key_edma").getString("scale")
            )
            key_krumhansl = scale2label(
                tonal.getJSONObject("key_krumhansl").getString("key"),
                tonal.getJSONObject("key_krumhansl").getString("scale")
            )
            key_temperley = scale2label(
                tonal.getJSONObject("key_temperley").getString("key"),
                tonal.getJSONObject("key_temperley").getString("scale")
            )
        } catch (e: Exception) {
            key_edma = scale2label(
                tonal.getString("key_key"),
                tonal.getString("key_scale")
            )
        }

        val chords_strength = stats(tonal.getJSONObject("chords_strength"))
        val hpcp_crest = try { stats(tonal.getJSONObject("hpcp_crest")) } catch (e: Exception) { Array(5) { -1.0 } }
        val hpcp_entropy = stats(tonal.getJSONObject("hpcp_entropy"))
        val tuning_nontempered_energy_ratio = tonal.getDouble("tuning_nontempered_energy_ratio")
        val tuning_diatonic_strength = tonal.getDouble("tuning_diatonic_strength")

        val rhythm: JSONObject = essentiaFeatures.getJSONObject("rhythm")
        val bpm = rhythm.getDouble("bpm")
        val danceability = rhythm.getDouble("danceability")
        val beats_loudness = stats(rhythm.getJSONObject("beats_loudness"))

        val metadata: JSONObject = essentiaFeatures.getJSONObject("metadata")
        val length = metadata.getJSONObject("audio_properties").getDouble("length")
        val replay_gain = metadata.getJSONObject("audio_properties").getDouble("replay_gain")

        val features = arrayOf(
            arrayOf(
                year, genre, length, replay_gain, dynamic_complexity, average_loudness,
                integrated_loudness, loudness_range, bpm, danceability, tuning_nontempered_energy_ratio,
                tuning_diatonic_strength
            ),
            momentary, short_term, lowlevelStats.flatten().toTypedArray(), key, key_edma, key_krumhansl, key_temperley,
            chords_strength, hpcp_crest, hpcp_entropy, beats_loudness
        ).flatten().toTypedArray()

        // log transform on features
        for ((i, feat) in features.withIndex()) {
            features[i] = if (feat < 0.0) -log10(-feat) else if (feat > 0.0) log10(feat) else 0.0
        }

        var finalFeatures = Array<Double>(highVarianceFeatureLabels.size) { 0.0 }
        for ((idx, fidx) in this.highVarianceFeatureLabels.withIndex()) {
            finalFeatures[idx] = features[fidx]
        }

        return finalFeatures
    }
}
