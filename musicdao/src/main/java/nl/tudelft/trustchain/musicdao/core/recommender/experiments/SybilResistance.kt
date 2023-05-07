package nl.tudelft.trustchain.musicdao.core.recommender.experiments

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.gossipML.models.feature_based.Adaline
import nl.tudelft.trustchain.musicdao.core.recommender.collaborativefiltering.UserBasedTrustedCollaborativeFiltering
import nl.tudelft.trustchain.musicdao.core.recommender.graph.*
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

fun main() {
    lateinit var trustNetwork: TrustNetwork
    val currentDir = System.getProperty("user.dir")
    val contextDir = "$currentDir/musicdao/src/test/resources"
    val usersFile = File("$contextDir/dataset/kaggle_users.txt")
    val loadedTrustNetwork = File("$contextDir/dataset/test_network.txt").readText()
    val subNetworks = Json.decodeFromString<SerializedSubNetworks>(loadedTrustNetwork)
    

}

