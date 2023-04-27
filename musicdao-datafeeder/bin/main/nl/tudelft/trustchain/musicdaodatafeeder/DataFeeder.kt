package nl.tudelft.trustchain.musicdaodatafeeder

import com.frostwire.jlibtorrent.TorrentInfo
import com.mpatric.mp3agic.Mp3File
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.script.Script
import java.io.File
import java.net.InetAddress
import java.util.*

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Usage: DataFeeder [path] <nopublish>")
        return
    }
    val dir = File(args[0])
    if (!dir.isDirectory) {
        println("$dir is not a directory")
        return
    }
    val disable = args[1]
    var publish = true
    if (disable == "nopublish") {
        publish = false
    }
    DataFeeder(dir, publish).start()
}

/**
 * @param musicDir a directory with directories (music albums/EPs/singles), in which the deep
 * directory should contain MP3 files
 */
class DataFeeder(private val musicDir: File, private val publish: Boolean) {
    fun start() {
        startIpv8()
        val ipv8Local = ipv8 ?: return
        scope.launch {
            while (true) {
                for ((_, overlay) in ipv8Local.overlays) {
                    printPeersInfo(overlay)
                }
                val musicCommunity = ipv8Local.getOverlay<MusicCommunity>() ?: return@launch
                musicCommunity.communicateReleaseBlocks()
                communicateSwarmHealth(musicCommunity)

                logger.info("===")
                delay(10000)
            }
        }
        // Wait until we have some peers, then send the proposal blocks
        Thread.sleep(10000)
        feed(musicDir)
        while (ipv8Local.isStarted()) {
            Thread.sleep(5000)
        }
    }

    private fun communicateSwarmHealth(musicCommunity: MusicCommunity) {
        val swarmHealthMap = musicCommunity.swarmHealthMap
        var count = 1
        val max = 5
        // Update all the timestamps to the current time, as we are still seeding all torrents
        val iterator = swarmHealthMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val infoHash = entry.key
            val swarmHealth = entry.value
            val swarmHealthUpdatedTimestamp = SwarmHealth(
                swarmHealth.infoHash,
                swarmHealth.numPeers, swarmHealth.numSeeds
            )
            swarmHealthMap[infoHash] = swarmHealthUpdatedTimestamp
        }
        for ((_, swarmHealth) in swarmHealthMap) {
            if (count > max) return
            // Refresh the timestamps
            musicCommunity.sendSwarmHealthMessage(swarmHealth)
            count += 1
        }
        return
    }

    private var ipv8: IPv8? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val logger = KotlinLogging.logger {}

    private fun createMusicCommunity(): OverlayConfiguration<MusicCommunity> {
        val settings = TrustChainSettings()
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        val database = Database(driver)
        val store = TrustChainSQLiteStore(database)
        val randomWalk = RandomWalk.Factory()
        return OverlayConfiguration(
            MusicCommunity.Factory(settings, store),
            listOf(randomWalk)
        )
    }

    private fun createDiscoveryCommunity(): OverlayConfiguration<DiscoveryCommunity> {
        val randomWalk = RandomWalk.Factory(timeout = 3.0, peers = 20)
        val randomChurn = RandomChurn.Factory()
        val periodicSimilarity = PeriodicSimilarity.Factory()
        return OverlayConfiguration(
            DiscoveryCommunity.Factory(),
            listOf(randomWalk, randomChurn, periodicSimilarity)
        )
    }

    private fun feed(dir: File) {
        if (!dir.isDirectory) return

        val community = ipv8?.getOverlay<MusicCommunity>() ?: return

        val walletMap = mutableMapOf<String, ECKey>() // Artist name, public Wallet Key

        val btcParams = CryptoCurrencyConfig.networkParams
        val allFiles = dir.listFiles() ?: return
        var count = 0
        for (albumFile in allFiles) {
            if (albumFile.isDirectory) {
                val audioFiles = albumFile.listFiles(AudioFileFilter()) ?: continue
                try {
                    val mp3File = Mp3File(audioFiles[0])
                    // TODO assuming they all have id3v2 tags?
                    var artist: String? = null
                    var title: String? = null
                    var year: String? = null
                    if (mp3File.hasId3v2Tag()) {
                        if (mp3File.id3v2Tag.albumArtist != null) {
                            artist = mp3File.id3v2Tag.albumArtist
                        }
                        if (mp3File.id3v2Tag.albumArtist != null) {
                            title = mp3File.id3v2Tag.album
                        }
                        if (mp3File.id3v1Tag.year != null) {
                            year = mp3File.id3v2Tag.year
                        }
                        if (artist == null && mp3File.id3v2Tag.artist != null) {
                            artist = mp3File.id3v2Tag.artist
                        }
                    }
                    if (mp3File.hasId3v1Tag()) {
                        if (artist == null && mp3File.id3v1Tag.artist != null) {
                            artist = mp3File.id3v1Tag.artist
                        }
                        if (title == null && mp3File.id3v1Tag.album != null) {
                            title = mp3File.id3v1Tag.album
                        }
                        if (title == null && mp3File.id3v1Tag.title != null) {
                            title = mp3File.id3v1Tag.title
                        }
                        if (year == null && mp3File.id3v1Tag.year != null) {
                            year = mp3File.id3v1Tag.year
                        }
                    }
                    if (artist == null || title == null || year == null) {
                        continue
                    } else {
                        count += 1
                        // Check if exists in map
                        // Optional create ipv8 id
                        // Create trustchain block
                        // Publish transaction
                        if (!walletMap.containsKey(artist)) {
                            // Create wallet
                            val key = ECKey()
                            walletMap[artist] = key
                        }
                        val bitcoinECKey = walletMap[artist] ?: continue
                        val bitcoinAddress =
                            Address.fromKey(btcParams, bitcoinECKey, Script.ScriptType.P2PKH)
                                .toString()

                        val list = mutableListOf<File>()
                        audioFiles.forEach {
                            list.add(it)
                        }
//                        val tor = SharedTorrent.create(albumFile, list, 65535, listOf(), "TrustChain-Superapp")
                        var torrentFile = "$albumFile.torrent"
                        if (!File(torrentFile).isFile) {
                            torrentFile = "$albumFile.torrent.added"
                        }
//                        tor.save(FileOutputStream(torrentFile))
                        val torrentInfo = TorrentInfo(File(torrentFile))
                        val magnet = torrentInfo.makeMagnetUri()
                        val torrentInfoName = torrentInfo.name()

                        val publicKey = community.myPeer.publicKey
                        val transaction = mutableMapOf<String, String>(
                            "magnet" to magnet,
                            "title" to title,
                            "artists" to artist,
                            "date" to year,
                            "torrentInfoName" to torrentInfoName,
                            "publisher" to bitcoinAddress
                        )
                        println("===")
                        for (entry in transaction) {
                            println("${entry.key} - ${entry.value}")
                        }
                        if (publish) {
                            community.createProposalBlock(
                                "publish_release",
                                transaction,
                                publicKey.keyToBin()
                            )
                        }
                        community.swarmHealthMap[torrentInfo.infoHash()] =
                            SwarmHealth(torrentInfo.infoHash().toString(), 0.toUInt(), 1.toUInt())
                    }
                } catch (e: Exception) {
                }
            }
        }

        println("count: $count")
        println("allfiles: ${allFiles.size}")
        println("artist name, public wallet key, private wallet key")
        for ((artist, key) in walletMap) {
            println("$artist,${key.publicKeyAsHex},${key.privateKeyAsHex}")
        }
    }

    private fun startIpv8() {
        val myKey = JavaCryptoProvider.generateKey()
        val myPeer = Peer(myKey)
        val udpEndpoint = UdpEndpoint(8090, InetAddress.getByName("0.0.0.0"))
        val endpoint = EndpointAggregator(udpEndpoint, null)

        val config = IPv8Configuration(
            overlays = listOf(
                createDiscoveryCommunity(),
                createMusicCommunity()
            ), walkerInterval = 1.0
        )

        val ipv8Local = IPv8(endpoint, config, myPeer)
        ipv8Local.start()
        ipv8 = ipv8Local
    }

    private fun printPeersInfo(overlay: Overlay) {
        val peers = overlay.getPeers()
        logger.info(overlay::class.simpleName + ": ${peers.size} peers")
        for (peer in peers) {
            val avgPing = peer.getAveragePing()
            val lastRequest = peer.lastRequest
            val lastResponse = peer.lastResponse

            val lastRequestStr = if (lastRequest != null)
                "" + ((Date().time - lastRequest.time) / 1000.0).toInt() + " s" else "?"

            val lastResponseStr = if (lastResponse != null)
                "" + ((Date().time - lastResponse.time) / 1000.0).toInt() + " s" else "?"

            val avgPingStr = if (!avgPing.isNaN()) "" + (avgPing * 1000).toInt() + " ms" else "? ms"
            logger.info("${peer.mid} (S: ${lastRequestStr}, R: ${lastResponseStr}, ${avgPingStr})")
        }
    }
}
