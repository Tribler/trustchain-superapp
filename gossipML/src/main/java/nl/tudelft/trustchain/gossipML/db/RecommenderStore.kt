package nl.tudelft.trustchain.gossipML.db

import android.annotation.SuppressLint
import android.util.Log
import nl.tudelft.trustchain.musicdaodatafeeder.AudioFileFilter
import com.mpatric.mp3agic.Mp3File
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.tudelft.gossipML.sqldelight.Database
import nl.tudelft.gossipML.sqldelight.Features
import nl.tudelft.gossipML.sqldelight.Unseen_features
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.trustchain.gossipML.Essentia
import nl.tudelft.trustchain.gossipML.models.Model
import nl.tudelft.trustchain.gossipML.models.collaborative_filtering.MatrixFactorization
import nl.tudelft.trustchain.gossipML.models.feature_based.Adaline
import nl.tudelft.trustchain.gossipML.models.feature_based.Pegasos
import org.json.JSONObject
import java.io.File
import kotlin.math.log10

/**
 * Handles database operations and preprocessing for local models and features
 */
open class RecommenderStore(
    private val musicStore: TrustChainSQLiteStore,
    private val database: Database
) {
    lateinit var key: ByteArray
    @SuppressLint("SdCardPath")
    private val musicDir = File("/data/user/0/nl.tudelft.trustchain/cache/")
    lateinit var essentiaJob: Job
    private var highVarianceFeatureLabels = arrayOf(3, 62, 162, 223, 130, 140)
    // 2 block features + cherry-picked essentia features
    private val totalAmountFeatures = highVarianceFeatureLabels.size + 2

    /**
     * save local model to the database
     *
     * @param model model to be stored
     */
    fun storeModelLocally(model: Model) {
        if (model.name == "MatrixFactorization") {
            database.dbModelQueries.addModel(
                name = model.name,
                type = model.name,
                parameters = (model as MatrixFactorization).serialize(private = true)
            )
        } else if (model.name == "Pegasos") {
            database.dbModelQueries.addModel(
                name = model.name,
                type = model.name,
                parameters = (model as Pegasos).serialize()
            )
        } else {
            database.dbModelQueries.addModel(
                name = model.name,
                type = model.name,
                parameters = (model as Adaline).serialize()
            )
        }
    }

    /**
     * loads model from the local database using name is a key
     *
     * @param name model name
     * @return loaded model
     */
    fun getLocalModel(name: String): Model {
        Log.i("Recommend", "Loading $name")
        val dbModel = database.dbModelQueries.getModel(name).executeAsOneOrNull()
        val model: Model
        if (name == "Adaline") {
            if (dbModel != null) {
                Log.i("Recommend", "Load existing local model")
                model = Json.decodeFromString<Adaline>(dbModel.parameters)
            } else {
                model = Adaline(0.1, totalAmountFeatures)
                Log.i("Recommend", "Initialized local model")
                Log.i("Recommend", model.name)
            }
        } else if (name == "Pegasos") {
            if (dbModel != null) {
                Log.i("Recommend", "Load existing local model")
                model = Json.decodeFromString<Pegasos>(dbModel.parameters)
            } else {
                model = Pegasos(0.01, totalAmountFeatures, 10)
                Log.i("Recommend", "Initialized local model")
                Log.i("Recommend", model.name)
            }
        } else {
            if (dbModel != null) {
                Log.i("Recommend", "Load existing local model")
                model = Json.decodeFromString<MatrixFactorization>(dbModel.parameters)
            } else {
                model = MatrixFactorization(Array(0) { "" }.zip(Array(0) { 0.0 }).toMap().toSortedMap())
                Log.i("Recommend", "Initialized local model")
                Log.i("Recommend", model.name)
            }
        }
        /**
         * we are trying to 'bias' our model with local preferences such that it is tuned for local user
         * thus, we re-train it on local dataset every time we load the model
         */
        val trainingData = getLocalSongData()
        if (trainingData.first.isNotEmpty()) {
            if (name == "Pegasos") {
                (model as Pegasos).update(trainingData.first, trainingData.second.map { it.toDouble() }.toTypedArray())
            } else if (name == "Adaline") {
                (model as Adaline).update(trainingData.first, trainingData.second.map { it.toDouble() }.toTypedArray())
            }
        }
        storeModelLocally(model)
        Log.i("Recommend", "Model completely loaded")
        return model
    }

    /**
     * get playcounts for local songs (i.e. get labels for local data)
     *
     * @return array of playcounts
     */
    fun getPlaycounts(): Array<Double> {
        val songBlocks = musicStore.getBlocksWithType("publish_release")
        val playcounts = Array(globalSongCount()) { 0.0 }
        for ((i, block) in songBlocks.withIndex()) {
            if (block.transaction["title"] != null && block.transaction["artist"] != null) {
                playcounts[i] = database.dbFeaturesQueries.getFeature(
                    "local-${block.transaction["title"]}-${block.transaction["artist"]}"
                )
                    .executeAsOneOrNull()?.count?.toDouble() ?: 0.0
            }
        }
        return playcounts
    }

    /**
     * extract song ids from trustchain blocks with music data
     *
     * @return distinct song ids
     */
    fun getSongIds(): Set<String> {
        val songBlocks = musicStore.getBlocksWithType("publish_release")
        val songIds = HashSet<String>()
        for (block in songBlocks) {
            if (block.transaction["title"] != null && block.transaction["artist"] != null) {
                songIds.add("${block.transaction["title"]}-${block.transaction["artist"]}")
            }
        }
        return songIds
    }

    /**
     * Update local features with features from new local file
     *
     * @param file unseen music file
     */
    fun updateLocalFeatures(file: File) {
        val mp3File = Mp3File(file)
        val k = if (mp3File.id3v2Tag != null)
            "local-${mp3File.id3v2Tag.title}-${mp3File.id3v2Tag.artist}"
        else if (mp3File.id3v1Tag != null)
            "local-${mp3File.id3v1Tag.title}-${mp3File.id3v1Tag.artist}"
        else
            return
        val existingFeature = database.dbFeaturesQueries.getFeature(key = k).executeAsOneOrNull()
        var count = 1
        if (existingFeature != null) {
            count = existingFeature.count.toInt() + 1
            Log.i("Recommend", "Song exists! Increment counter")
        }
        val mp3Features = extractMP3Features(mp3File)
        database.dbFeaturesQueries.addFeature(
            key = k,
            songFeatures = mp3Features.contentToString(),
            count = count.toLong()
        )
    }

    /**
     * extracts music block metadata and transforms it to array of features
     * that can be directly fed into ml models
     *
     * @param block music block
     * @return array of features for given instance
     */
    private fun blockMetadata(block: TrustChainBlock): Array<Double> {
        var year = -1.0
        var genre = -1.0
        val k: String

        if (block.transaction["title"] != null && block.transaction["artist"] != null) {
            k = "local-${block.transaction["title"]}-${block.transaction["artist"]}"
            val unseenFeatures = this.database.dbUnseenFeaturesQueries.getFeature(k).executeAsOneOrNull()
            if (unseenFeatures != null) {
                return Json.decodeFromString<DoubleArray>(unseenFeatures.songFeatures!!).toTypedArray()
            }
        }

        if (block.transaction["date"] != null) {
            try {
                year = Integer.parseInt(block.transaction["date"] as String).toDouble()
            } catch (e: Exception) {
            }

            try {
                // Ectrat year from string format '01/02/2020'
                year = Integer.parseInt(
                    (
                        block.transaction["date"] as
                            String
                        ).split("/").toTypedArray()[-1]
                ).toDouble()
            } catch (e: Exception) {
            }
        }

        if (block.transaction["genre"] != null) {
            genre = block.transaction["genre"] as Double
        }

        return arrayOf(year, genre) + Array(223) { -1.0 }
    }

    /**
     * get amount of songs from trustchain for which we can extract features
     *
     * @return amount of test songs
     */
    fun globalSongCount(): Int {
        return getSongBlockMetadata(musicStore.getBlocksWithType("publish_release")).size
    }

    private fun getSongBlockMetadata(songsHistory: List<TrustChainBlock>): Array<Array<Double>> {
        val features = Array(songsHistory.size) { Array(totalAmountFeatures) { 0.0 } }
        for (i in songsHistory.indices) {
            features[i] = blockMetadata(songsHistory[i])
        }
        return features
    }

    /**
     * process local files adnd add songs features to the db
     */
    fun addAllLocalFeatures() {
        if (!musicDir.isDirectory) return

        val allFiles = musicDir.listFiles() ?: return
        Log.i("Recommend", "Amount of files is ${allFiles.size}")

        var idx = 0
        for (albumFile in allFiles) {
            Log.i("Recommend", "Local album is ${albumFile.name}")
            if (albumFile.isDirectory) {
                val audioFiles = albumFile.listFiles(AudioFileFilter()) ?: continue
                Log.i("Recommend", "Local songs amount in alum: ${audioFiles.size}")
                for (f in audioFiles) {
                    if (Mp3File(f).id3v2Tag != null) {
                        val updatedFile = Mp3File(f)

                        val k = "local-${updatedFile.id3v2Tag.title}-${updatedFile.id3v2Tag.artist}"
                        Log.i("Recommend", "${updatedFile.filename} haveFeature: ${haveFeature(k, zerosFine = false)}")
                        if (!haveFeature(k, zerosFine = false)) {
                            try {
                                val mp3Features = extractMP3Features(Mp3File(f))
                                val count = 1
                                database.dbFeaturesQueries.addFeature(
                                    key = k,
                                    songFeatures = mp3Features.contentToString(),
                                    count = count.toLong()
                                )
                            } catch (e: Exception) {
                                Log.e("Recommend", "Extracting audio features failed!")
                                Log.e("Recommend", e.toString())
                            }
                        }
                        idx += 1
                    }
                }
            }
        }
    }

    /**
     * this method is used upon reception of gossiped features (by other peers)
     * process received features and store to the table of unseen features
     * if don't have this song locally
     *
     * @param songIdentifier song id
     * @param features song feature
     */
    fun addNewFeatures(songIdentifier: String, features: String) {
        val seen = database.dbFeaturesQueries.getFeature(songIdentifier).executeAsOneOrNull()
        val unseen = database.dbUnseenFeaturesQueries.getFeature(songIdentifier).executeAsOneOrNull()
        if (seen == null && unseen == null) {
            database.dbUnseenFeaturesQueries.addFeature(
                key = songIdentifier,
                songFeatures = features
            )
        }
    }

    /**
     * get local song data for model training
     *
     * @return training data (pair of features and labels) from local songs
     */
    fun getLocalSongData(): Pair<Array<Array<Double>>, IntArray> {
        if (!essentiaJob.isActive)
            essentiaJob = GlobalScope.launch { addAllLocalFeatures() } // analyze local music files
        val batch = database.dbFeaturesQueries.getAllFeatures().executeAsList()
        if (batch.isEmpty()) {
            Log.w(
                "Recommend",
                "Local feature database is empty! " +
                    "Analyzing files in background thread now, current recommendation will be empty."
            )
            return Pair(emptyArray(), emptyArray<Int>().toIntArray())
        }
        val features = Array(batch.size) { Array(totalAmountFeatures) { 0.0 } }
        val playcounts = Array(batch.size) { 0 }.toIntArray()
        for (i in batch.indices) {
            features[i] = Json.decodeFromString<DoubleArray>(batch[i].songFeatures!!).toTypedArray()
            playcounts[i] = batch[i].count.toInt()
        }
        return Pair(features, playcounts)
    }

    /**
     * load new songs from trustchain
     *
     * @return pair of new song's features and corresponding trustchain blocks
     */
    fun getNewSongs(): Pair<Array<Array<Double>>, List<TrustChainBlock>> {
        val songsHistory = musicStore.getBlocksWithType("publish_release")
        val data = getSongBlockMetadata(songsHistory)
        return Pair(data, songsHistory)
    }

    /**
     * extracts mp3features from a given file using Essentia
     *
     * @param mp3File file with song
     * @return features
     */
    private fun extractMP3Features(mp3File: Mp3File): Array<Double> {
        var features = Array(225) { -1.0 }
        val year = (mp3File.id3v2Tag ?: mp3File.id3v1Tag)?.year?.toDouble() ?: -1.0
        val genre = (mp3File.id3v2Tag ?: mp3File.id3v1Tag)?.genre?.toDouble() ?: -1.0
        try {
            val filename = mp3File.filename

            val k = if (mp3File.id3v2Tag != null)
                "local-${mp3File.id3v2Tag.title}-${mp3File.id3v2Tag.artist}"
            else if (mp3File.id3v1Tag != null)
                "local-${mp3File.id3v1Tag.title}-${mp3File.id3v1Tag.artist}"
            else
                ""

            if (haveFeature(k)) {
                val featureText = database.dbFeaturesQueries.getFeature(k).executeAsOneOrNull()
                    ?.songFeatures
                    ?: database.dbUnseenFeaturesQueries.getFeature(k).executeAsOneOrNull()?.songFeatures
                return Json.decodeFromString<DoubleArray>(featureText!!).toTypedArray()
            }

            val jsonfile = filename.replace(".mp3", ".json")

            if (!File(jsonfile).exists()) {
                if (Essentia.extractData(filename, jsonfile) == 1) {
                    Log.e("Recommend", "Error extracting data with Essentia")
                } else {
                    Log.i("Recommend", "Got essentia features for $filename")
                }
            }
            val essentiaFeatures = JSONObject(File(jsonfile).bufferedReader().use { it.readText() })
            val keys = arrayOf(
                "barkbands_crest", "barkbands_flatness_db", "barkbands_kurtosis", "barkbands_skewness", "barkbands_spread",
                "dissonance", "erbbands_crest", "erbbands_flatness_db", "erbbands_kurtosis", "erbbands_skewness",
                "erbbands_spread", "hfc", "melbands_crest", "melbands_flatness_db", "melbands_kurtosis", "melbands_skewness",
                "melbands_spread", "pitch_salience", "spectral_centroid", "spectral_complexity", "spectral_decrease",
                "spectral_energy", "spectral_energyband_high", "spectral_energyband_low", "spectral_energyband_middle_high",
                "spectral_energyband_middle_low", "spectral_entropy", "spectral_flux", "spectral_kurtosis", "spectral_rms",
                "spectral_rolloff", "spectral_skewness", "spectral_spread", "spectral_strongpeak", "zerocrossingrate"
            )

            var dynamic_complexity = 0.0
            var average_loudness = 0.0
            var integrated_loudness = 0.0
            var loudness_range = 0.0
            var momentary = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
            var short_term = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
            val lowlevelStats = Array(keys.size) { Array(5) { -1.0 } }

            if (essentiaFeatures.has("lowlevel")) {
                val lowlevel = essentiaFeatures.getJSONObject("lowlevel")
                try {
                    dynamic_complexity = lowlevel.getDouble("dynamic_complexity")
                    average_loudness = lowlevel.getDouble("average_loudness")
                    integrated_loudness = lowlevel.getJSONObject("loudness_ebu128").getDouble("integrated")
                    loudness_range = lowlevel.getJSONObject("loudness_ebu128").getDouble("loudness_range")
                    momentary = stats(lowlevel.getJSONObject("loudness_ebu128").getJSONObject("momentary"))
                    short_term = stats(lowlevel.getJSONObject("loudness_ebu128").getJSONObject("short_term"))

                    for ((i, key) in keys.withIndex()) {
                        lowlevelStats[i] = stats(lowlevel.getJSONObject(key))
                    }
                } catch (e: java.lang.Exception) {
                    Log.e("Recommend", e.toString())
                }
            }

            val tonal: JSONObject
            var key = arrayOf(0.0, 0.0)
            var key_edma = arrayOf(0.0, 0.0)
            var key_krumhansl = arrayOf(0.0, 0.0)
            var key_temperley = arrayOf(0.0, 0.0)
            var chords_strength = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
            var hpcp_crest = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
            var hpcp_entropy = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
            var tuning_nontempered_energy_ratio = 0.0
            var tuning_diatonic_strength = 0.0
            if (essentiaFeatures.has("tonal")) {
                tonal = essentiaFeatures.getJSONObject("tonal")
                try {
                    key = scale2label(tonal.getString("chords_key"), tonal.getString("chords_scale"))
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
                    chords_strength = stats(tonal.getJSONObject("chords_strength"))
                    hpcp_crest = stats(tonal.getJSONObject("hpcp_crest"))
                    hpcp_entropy = stats(tonal.getJSONObject("hpcp_entropy"))
                    tuning_nontempered_energy_ratio = tonal.getDouble("tuning_nontempered_energy_ratio")
                    tuning_diatonic_strength = tonal.getDouble("tuning_diatonic_strength")
                } catch (e: java.lang.Exception) {
                    Log.e("Recommend", e.toString())
                }
            }

            val rhythm: JSONObject
            var bpm = 0.0
            var danceability = 0.0
            var beats_loudness = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
            if (essentiaFeatures.has("rhythm")) {
                try {
                    rhythm = essentiaFeatures.getJSONObject("rhythm")
                    bpm = rhythm.getDouble("bpm")
                    danceability = rhythm.getDouble("danceability")
                    beats_loudness = stats(rhythm.getJSONObject("beats_loudness"))
                } catch (e: java.lang.Exception) {
                    Log.e("Recommend", e.toString())
                }
            }

            val metadata: JSONObject
            var length = 0.0
            var replay_gain = 0.0
            if (essentiaFeatures.has("rhythm")) {
                metadata = essentiaFeatures.getJSONObject("metadata")
                try {
                    length = metadata.getJSONObject("audio_properties").getDouble("length")
                    replay_gain = metadata.getJSONObject("audio_properties").getDouble("replay_gain")
                } catch (e: java.lang.Exception) {
                    Log.e("Recommend", e.toString())
                }
            }

            features = arrayOf(
                arrayOf(
                    year, genre, length, replay_gain, dynamic_complexity, average_loudness,
                    integrated_loudness, loudness_range, bpm, danceability, tuning_nontempered_energy_ratio,
                    tuning_diatonic_strength
                ),
                momentary, short_term, lowlevelStats.flatten().toTypedArray(), key, key_edma, key_krumhansl, key_temperley,
                chords_strength, hpcp_crest, hpcp_entropy, beats_loudness
            ).flatten().toTypedArray()
        } catch (e: Exception) {
            Log.e("Recommend", "Essentia extraction failed:")
            Log.e("Recommend", e.stackTraceToString())
        }

        // log transform on features
        for ((i, feat) in features.withIndex()) {
            features[i] = if (feat < 0.0) -log10(-feat) else if (feat > 0.0) log10(feat) else 0.0
        }
        Log.i("Recommend", "Extracted MP3 features")

        /**
         * pick most 'promising' Essentia features, read docs for more insight
         * we still keep 2 block features - year and genre,
         * in order to have some data for completely unseen features
         */
        val finalFeatures = Array(totalAmountFeatures) { 0.0 }
        finalFeatures[0] = year
        finalFeatures[1] = genre
        for ((idx, fidx) in this.highVarianceFeatureLabels.withIndex()) {
            finalFeatures[idx] = features[fidx + 1]
        }

        return finalFeatures
    }

    fun getMyFeatures(): List<Features> {
        return this.database.dbFeaturesQueries.getAllFeatures().executeAsList()
    }

    private fun getRemoteFeatures(): List<Unseen_features> {
        return this.database.dbUnseenFeaturesQueries.getAllFeatures().executeAsList()
    }

    private fun haveFeature(key: String, zerosFine: Boolean = true): Boolean {
        for (feat in getMyFeatures())
            if (feat.key == key) {
                return if (zerosFine) true else {
                    for ((i, d) in Json.decodeFromString<DoubleArray>(feat.songFeatures!!).toTypedArray().withIndex()) {
                        if (i >= 2 && d != 0.0)
                            return true
                    }
                    return false
                }
            }
        for (feat in getRemoteFeatures())
            if (feat.key == key) {
                return if (zerosFine) true else {
                    for ((i, d) in Json.decodeFromString<DoubleArray>(feat.songFeatures!!).toTypedArray().withIndex()) {
                        if (i >= 2 && d != 0.0)
                            return true
                    }
                    return false
                }
            }
        return false
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: RecommenderStore
        fun getInstance(musicStore: TrustChainSQLiteStore, database: Database): RecommenderStore {
            if (!Companion::instance.isInitialized) {
                instance = RecommenderStore(musicStore, database)
            }
            return instance
        }
    }
}

// positions of scales on the circle of fifths
val majorKeys = mapOf(
    "C" to 0.0, "G" to 1.0, "D" to 2.0, "A" to 3.0, "E" to 4.0, "B" to 5.0, "Gb" to 6.0,
    "F#" to 6.0, "Db" to 7.0, "Ab" to 8.0, "Eb" to 9.0, "Bb" to 10.0, "F" to 11.0
)
val minorKeys = mapOf(
    "A" to 0.0, "E" to 1.0, "B" to 2.0, "F#" to 3.0, "C#" to 4.0, "G#" to 5.0, "D#" to 6.0,
    "Eb" to 6.0, "Bb" to 7.0, "F" to 8.0, "C" to 9.0, "G" to 10.0, "D" to 11.0
)
fun scale2label(key: String, mode: String): Array<Double> {
    val keyCode = (if (mode == "major") majorKeys[key] else minorKeys[key]) ?: -1.0
    val modeCode = if (mode == "major") 0.0 else 1.0
    return arrayOf(keyCode, modeCode)
}
fun stats(obj: JSONObject): Array<Double> {
    return arrayOf(
        obj.getDouble("min"),
        obj.getDouble("median"),
        obj.getDouble("mean"),
        obj.getDouble("max"),
        obj.getDouble("var"),
    )
}
