package nl.tudelft.trustchain.musicdaodatafeeder

import com.frostwire.jlibtorrent.TorrentInfo
import com.mpatric.mp3agic.Mp3File
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.ipv8.*
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.messaging.EndpointAggregator
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.peerdiscovery.strategy.PeriodicSimilarity
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomChurn
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.ipv8.util.random
import java.io.File
import java.net.InetAddress
import java.util.*

/*
This file has been extracted from DataSeeder.kt

DataPropagator.kt can only find albums with the torrent file and propagate them to the network.
Seeding part from DataSeeder.kt has been removed.

How to run:
1. Set up DataPropagator.kt as main class in the build.gradle file
2. Run:
```
    ./gradlew :musicdao-datafeeder:run --args="/home/folder"
```

*/

val logger = KotlinLogging.logger {}

fun musicCommunity(): OverlayConfiguration<MusicCommunity> {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    Database.Schema.create(driver)
    return OverlayConfiguration(
        factory = MusicCommunity.Factory(
            settings = TrustChainSettings(),
            database = TrustChainSQLiteStore(Database(driver))
        ),
        walkers = listOf(RandomWalk.Factory())
    )
}

fun discoveryCommunity() = OverlayConfiguration(
    factory = DiscoveryCommunity.Factory(),
    walkers = listOf(
        RandomWalk.Factory(timeout = 3.0, peers = 20),
        RandomChurn.Factory(),
        PeriodicSimilarity.Factory()
    )
)

fun ipv8() = IPv8(
    endpoint = EndpointAggregator(
        udpEndpoint = UdpEndpoint(
            port = 8090,
            ip = InetAddress.getByName("0.0.0.0")
        ), bluetoothEndpoint = null
    ),
    configuration = IPv8Configuration(
        overlays = listOf(
            discoveryCommunity(),
            musicCommunity()
        ), walkerInterval = 1.0
    ),
    myPeer = Peer(JavaCryptoProvider.generateKey())
)

fun extractTorrentInfo(file: File): Pair<String, String> {
    val torrentInfo = TorrentInfo(file)
    return Pair(torrentInfo.makeMagnetUri(), torrentInfo.name())
}

fun extractMusicInfo(file: File): Triple<String, String, String> {
    val mp3File = Mp3File(file)
    val artist = sequenceOf(
        mp3File.id3v2Tag?.albumArtist,
        mp3File.id3v2Tag?.artist,
        mp3File.id3v1Tag?.artist
    ).firstOrNull { it != null } ?: ""

    val title = sequenceOf(
        mp3File.id3v2Tag?.album,
        mp3File.id3v1Tag?.album
    ).firstOrNull { it != null } ?: ""

    val year = sequenceOf(
        mp3File.id3v2Tag?.year,
        mp3File.id3v1Tag?.year
    ).firstOrNull { it != null } ?: ""

    return Triple(artist, title, year)
}

fun fillDatabase(folder: File, ipv8: IPv8) {
    folder.walkTopDown().filter { it.isDirectory }.forEach { album ->
        logger.info { "Album: $album" }
        val torrentFile = File("$album.torrent")
        if (!torrentFile.exists()) {
            logger.warn { "$torrentFile doesn't exists" }
            return@forEach
        }

        val (magnet, torrentName) = extractTorrentInfo(torrentFile)

        val firstTrack = album.walkTopDown().firstOrNull { it.name.endsWith("mp3") }
        if (firstTrack == null) {
            logger.warn { "First track doesn't exists" }
            return@forEach
        }

        logger.info { "First track: $firstTrack" }
        val (artist, title, year) = extractMusicInfo(firstTrack)

        val community = ipv8.getOverlay<MusicCommunity>()!!
        val transaction = mutableMapOf(
            "magnet" to magnet,
            "title" to title,
            "artists" to artist,
            "date" to year,
            "torrentInfoName" to torrentName,
            "publisher" to ""
        )
        logger.info { "Transaction: $transaction" }
        community.createProposalBlock(
            blockType = "publish_release",
            transaction = transaction,
            publicKey = community.myPeer.publicKey.keyToBin()
        )
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun propagateBlocks(ipv8: IPv8) {
    val community = ipv8.getOverlay<MusicCommunity>()!!
    val releaseBlocks = community.database.getBlocksWithType("publish_release")
    val maxBlocksCount = 3
    val delayMillis = 5000L
    while (true) {
        val totalPeers = community.getPeers()
        val realPeers = totalPeers.filter { !Community.DEFAULT_ADDRESSES.contains(it.address) }
        logger.info { "Peers count: ${totalPeers.count()}/${realPeers.count()}" }

        realPeers.randomOrNull()?.let { peer ->
            releaseBlocks.random(maxBlocksCount).forEach {
                logger.info { "Sending:\nPeer: $peer.\nTransaction: ${it.transaction}" }
                community.sendBlock(block = it, peer = peer)
            }
        }
        logger.info { "Waiting for $delayMillis Millis" }
        Thread.sleep(delayMillis)
    }
}

fun main(args: Array<String>) {
    logger.info { "Run with arguments: ${args.joinToString(",")}" }
    logger.info { "Bootstrap servers: ${Community.DEFAULT_ADDRESSES}" }

    val ipv8 = ipv8()
    logger.info { "Start ipv8" }
    ipv8.start()

    fillDatabase(folder = File(args[0]), ipv8 = ipv8)
    propagateBlocks(ipv8 = ipv8)
}
