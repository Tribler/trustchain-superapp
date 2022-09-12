package nl.tudelft.trustchain.literaturedao

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.eva.PeerBusyException
import nl.tudelft.ipv8.messaging.eva.TimeoutException
import nl.tudelft.ipv8.messaging.eva.TransferType
import nl.tudelft.trustchain.literaturedao.utils.ExtensionUtils.Companion.supportedAppExtensions
import nl.tudelft.trustchain.literaturedao.utils.ExtensionUtils.Companion.torrentExtension
import nl.tudelft.trustchain.literaturedao.utils.MagnetUtils.Companion.addressTracker
import nl.tudelft.trustchain.literaturedao.utils.MagnetUtils.Companion.addressTrackerAppender
import nl.tudelft.trustchain.literaturedao.utils.MagnetUtils.Companion.constructMagnetLink
import nl.tudelft.trustchain.literaturedao.utils.MagnetUtils.Companion.displayNameAppender
import nl.tudelft.trustchain.literaturedao.utils.MagnetUtils.Companion.magnetHeaderString
import nl.tudelft.trustchain.literaturedao.utils.MagnetUtils.Companion.preHashString
import nl.tudelft.trustchain.literaturedao.ipv8.LiteraturePayload
import nl.tudelft.trustchain.literaturedao.ipv8.EvaDownload
import nl.tudelft.trustchain.literaturedao.ipv8.LitDaoMessage
import nl.tudelft.trustchain.literaturedao.ipv8.LiteratureCommunity
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.Pair
import kotlin.collections.HashMap

/**
 * This gossips data about 5 random pdfs with peers on literatureCommunity every 10 seconds and
 * fetches new apps from peers every 20 seconds
 */

lateinit var literatureGossiperInstance: LiteratureGossiper
const val GOSSIP_DELAY: Long = 10000
const val DOWNLOAD_DELAY: Long = 20000

const val TORRENT_ATTEMPTS_THRESHOLD = 1
const val EVA_RETRIES = 10
private val logger = KotlinLogging.logger {}


class LiteratureGossiper(
    private val sessionManager: SessionManager,
    private val activity: LiteratureDaoActivity,
    private val literatureCommunity: LiteratureCommunity,
    private val toastingEnabled: Boolean
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var evaDownload: EvaDownload = EvaDownload()
    private val maxTorrentThreads = 5
    private val torrentHandles = ArrayList<TorrentHandle>()
    private val torrentInfos = ArrayList<TorrentInfo>()
    private val failedTorrents = HashMap<String, Int>()
    internal var signal = CountDownLatch(0)
    private val appDirectory = activity.applicationContext.cacheDir
    private var gossipingPaused = false
    private var downloadingPaused = false
    var sessionActive = false
    var downloadsInProgress = 0
    var downloadsFinished = 0

    companion object {
        fun getInstance(
            sessionManager: SessionManager,
            activity: LiteratureDaoActivity,
            literatureCommunity: LiteratureCommunity,
            toastingEnabled: Boolean = true
        ): LiteratureGossiper {
            if (!::literatureGossiperInstance.isInitialized) {
                literatureGossiperInstance = LiteratureGossiper(
                    sessionManager, activity,
                    literatureCommunity, toastingEnabled
                )
            }
            return literatureGossiperInstance
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun start() {
        printToast("Start gossiper")
        // This call seems redundant, but it's necessary to populate known torrents
        // before downloading starts so we don't retry known downloads
        populateKnownTorrents()
        initializeTorrentSession()
        // TODO: Fix for lower Android API levels.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            initializeEvaCallbacks()
        } else {
            throw NotImplementedError("Eva is not supported on Android versions below M")
        }
        initialUISettings()
        val sp = SettingsPack()
        sp.seedingOutgoingConnections(true)
        val params =
            SessionParams(sp)
        sessionManager.start(params)
        scope.launch {
            // TODO: Fix for lower Android API levels.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                iterativelyShareLiteratures()
            } else {
                throw NotImplementedError("Literature sharing is not supported on this device")
            }
        }
        scope.launch {
            // TODO: Fix for lower Android API levels.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                iterativelyDownloadLiteratures()
            } else {
                throw NotImplementedError("Android version not supported")
            }
        }
    }

    fun addTorrentInfo(torrentInfo: TorrentInfo) {
        this.torrentInfos.add(torrentInfo)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initializeEvaCallbacks() {
        // TODO: Fix for lower Android API levels.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            literatureCommunity.setEVAOnReceiveCompleteCallback { peer, _, _, data ->
                data?.let {
                    val packet = Packet(peer.address, data)
                    val (_, payload) = packet.getDecryptedAuthPayload(
                        LiteraturePayload.Deserializer, literatureCommunity.myPeer.key as PrivateKey
                    )
                    evaDownload.activeDownload = false
                    activity.runOnUiThread {
                        printToast("Torrent ${payload.literatureName} fetched through EVA protocol!")
                        onDownloadSuccess(payload.literatureName)
                    }
                }
            }
        } else {
            throw NotImplementedError("EVA protocol not supported on Android versions below Nougat")
        }

        literatureCommunity.setEVAOnReceiveProgressCallback { _, _, progress ->
            updateProgress(progress = progress.progress.toInt())
        }

        literatureCommunity.setEVAOnErrorCallback { _, exception ->
            if (evaDownload.activeDownload && exception.transfer?.type == TransferType.INCOMING) {
                if (exception is TimeoutException || exception is PeerBusyException) {
                    activity.runOnUiThread { printToast("Failed to fetch through EVA protocol because $exception! Retrying") }
                    retryActiveEvaDownload()
                } else {
                    activity.runOnUiThread { printToast("Can't fetch through EVA because of $exception will continue to retry via torrent") }
                    evaDownload.activeDownload = false
                    downloadHasFailed()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun retryActiveEvaDownload() {
        if (evaDownload.retryAttempts < EVA_RETRIES) {
            evaDownload.peer?.let {
                // TODO: Fix for lower Android API levels.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    literatureCommunity.sendLiteratureRequest(evaDownload.magnetInfoHash, it)
                } else {
                    throw NotImplementedError("EVA protocol not supported on Android versions below Nougat")
                }
            }
            evaDownload.lastRequest = System.currentTimeMillis()
            evaDownload.retryAttempts++
            if (evaDownload.retryAttempts == EVA_RETRIES) {
                activity.runOnUiThread { printToast("Giving up on trying to download via EVA") }
                evaDownload.activeDownload = false
                downloadHasFailed()
            } else {
                downloadHasStarted(evaDownload.fileName)
            }
            activity.runOnUiThread {
                logger.info { "Retrying download" }
            }
        }
    }

    fun pause() {
        gossipingPaused = true
        downloadingPaused = true
    }

    fun resume() {
        initialUISettings()
        gossipingPaused = false
        downloadingPaused = false
    }

    private fun initializeTorrentSession() {
        sessionManager.addListener(object : AlertListener {
            override fun types(): IntArray? {
                return null
            }

            override fun alert(alert: Alert<*>) {
                when (alert.type()) {
                    AlertType.ADD_TORRENT -> {
                        logger.info { "Torrent added" }
                        (alert as AddTorrentAlert).handle().resume()
                    }
                    AlertType.BLOCK_FINISHED -> {
                        val a = alert as BlockFinishedAlert
                        val p = (a.handle().status().progress() * 100).toInt()
                        logger.info { "Progress: " + p + " for torrent name: " + a.torrentName() }
                        printToast("Download in progress!")
                        updateProgress(p)
                        logger.info {
                            java.lang.Long.toString(
                                sessionManager.stats().totalDownload()
                            )
                        }
                    }
                    AlertType.TORRENT_FINISHED -> {
                        signal.countDown()
                        logger.info { "Torrent finished" }
                    }
                    else -> {
                    }
                }
            }
        })
    }

    /**
     * This is a very simplistic way to crawl all chains from the peers you know
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun iterativelyShareLiteratures() {
        while (scope.isActive) {
            if (!gossipingPaused) {
                try {
                    // TODO: implement for lower API levels.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        randomlyShareFiles()
                    } else {
                        throw NotImplementedError("Literature sharing not supported on Android versions below Marshmallow")
                    }
                } catch (e: Exception) {
                    activity.runOnUiThread { printToast(e.toString()) }
                }
            }
            delay(GOSSIP_DELAY)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun iterativelyDownloadLiteratures() {
        while (scope.isActive) {
            if (!downloadingPaused) {
                try {
                    downloadPendingFiles()
                    if (evaDownload.activeDownload && evaDownload.lastRequest?.let { it -> System.currentTimeMillis() - it } ?: 0 > 30 * 1000) {
                        activity.runOnUiThread { printToast("EVA Protocol timed out, retrying") }
                        // TODO: fix for lower API levels.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            retryActiveEvaDownload()
                        } else {
                            throw NotImplementedError("EVA protocol not supported on Android versions below Nougat")
                        }
                    }
                } catch (e: Exception) {
                    activity.runOnUiThread { printToast(e.toString()) }
                }
            }
            delay(DOWNLOAD_DELAY)
        }
    }

    fun addButtonsInAdvance(packets: ArrayList<Pair<Peer, LitDaoMessage>>) {
        for (packet in packets) {
            val peer = packet.first
            val payload = packet.second
            logger.info { peer.mid + ": " + payload.message }
            activity.runOnUiThread {
                // @TODO: Add item to local data
                //      val torrentName = payload.message.substringAfter("&dn=")
                //                .substringBefore('&')
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun downloadPendingFiles() {
        // TODO: Fix for lower Android API levels.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            addDownloadToQueue(literatureCommunity.torrentMessagesList.size - downloadsFinished)
            addButtonsInAdvance(literatureCommunity.torrentMessagesList)
            for ((peer, payload) in ArrayList(literatureCommunity.torrentMessagesList)) {
                val torrentName = payload.message.substringAfter(displayNameAppender)
                    .substringBefore('&')
                val magnetLink = payload.message.substringAfter("LitDao:")
                val torrentHash = magnetLink.substringAfter(preHashString)
                    .substringBefore(displayNameAppender)
                if (torrentInfos.none { it.infoHash().toString() == torrentHash }) {
                    if (failedTorrents.containsKey(torrentName)) {
                        // Wait at least 1000 seconds if torrent failed before
                        if (failedTorrents[torrentName]!! >= TORRENT_ATTEMPTS_THRESHOLD)
                            continue
                    }
                    getMagnetLink(magnetLink, torrentName, peer)
                }
            }
        } else {
            throw NotImplementedError("Literature sharing not supported on Android versions below Nougat")
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun randomlyShareFiles() {
        literatureCommunity.run {
            populateKnownTorrents()
            torrentInfos.shuffle()
            val toSeed: ArrayList<TorrentInfo> = ArrayList(torrentInfos.take(maxTorrentThreads))
            torrentHandles.forEach { torrentHandle ->
                if (toSeed.any { it.infoHash() == torrentHandle.infoHash() }) {
                    val dup = toSeed.find { it.infoHash() == torrentHandle.infoHash() }
                    toSeed.remove(dup)
                    if (dup != null) {
                        val magnetLink = constructMagnetLink(dup.infoHash(), dup.name())
                        // TODO: Fix for lower Android API levels.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            informAboutTorrent(magnetLink)
                        } else {
                            throw NotImplementedError("Not implemented for Android versions below Nougat")
                        }
                    }
                } else
                    torrentHandle.pause()
            }
            toSeed.forEach {
                downloadAndSeed(it)
            }
        }
    }

    private fun populateKnownTorrents() {
        appDirectory.listFiles()?.forEachIndexed { _, file ->
            if (file.name.endsWith(torrentExtension)) {
                TorrentInfo(file).let { torrentInfo ->
                    if (torrentInfo.isValid) {
                        if (isTorrentOkay(torrentInfo, appDirectory)) {
                            if (!torrentInfos.any { it.infoHash() == torrentInfo.infoHash() })
                                torrentInfos.add(torrentInfo)
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun downloadAndSeed(torrentInfo: TorrentInfo) {
        if (torrentInfo.isValid) {
            sessionManager.download(torrentInfo, appDirectory)
            sessionManager.find(torrentInfo.infoHash())?.let { torrentHandle ->
                torrentHandle.setFlags(torrentHandle.flags().and_(TorrentFlags.SEED_MODE))
                torrentHandle.pause()
                torrentHandle.resume()
                // This is a fix/hack that forces SEED_MODE to be available, for
                // an unsolved issue: seeding local torrents often result in an endless "CHECKING_FILES"
                // state
                val magnetLink = constructMagnetLink(torrentInfo.infoHash(), torrentInfo.name())
                // TODO: Fix for lower Android API levels.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    literatureCommunity.informAboutTorrent(magnetLink)
                } else {
                    throw NotImplementedError("This version of Android is not supported")
                }
                torrentHandles.add(torrentHandle)
            }
        }
    }

    private fun isTorrentOkay(torrentInfo: TorrentInfo, saveDirectory: File): Boolean {
        File(saveDirectory.path + "/" + torrentInfo.name()).run {
            if (!supportedAppExtensions.contains(extension)) return false
            if (length() >= torrentInfo.totalSize()) return true
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getMagnetLink(magnetLink: String, torrentName: String, peer: Peer) {
        // Handling of the case where the user is already downloading the
        // same or another torrent

        if (sessionActive || !magnetLink.startsWith(magnetHeaderString) || evaDownload.activeDownload)
            return

        downloadHasStarted(torrentName)

        activity.runOnUiThread {
            printToast("Found new torrent $torrentName attempting to download!")
        }
        val startIndexName = magnetLink.indexOf(displayNameAppender)
        val stopIndexName =
            if (magnetLink.contains(addressTrackerAppender)) magnetLink.indexOf(addressTracker) else magnetLink.length

        val magnetNameRaw = magnetLink.substring(startIndexName + 4, stopIndexName)
        logger.info { magnetNameRaw }
        val magnetName = magnetNameRaw.replace('+', ' ', false)
        val magnetInfoHash = magnetLink.substring(preHashString.length, startIndexName)
        logger.info { magnetName }

        val sp = SettingsPack()
        sp.seedingOutgoingConnections(true)
        val params =
            SessionParams(sp)
        sessionManager.start(params)

        val timer = Timer()
        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    val nodes = sessionManager.stats().dhtNodes()
                    // wait for at least 10 nodes in the DHT.
                    if (nodes >= 10) {
                        logger.info { "DHT contains $nodes nodes" }
                        // signal.countDown()
                        timer.cancel()
                    }
                }
            },
            0, 1000
        )

        logger.info { "Fetching the magnet uri, please wait..." }
        val data: ByteArray
        try {
            data = sessionManager.fetchMagnet(magnetLink, 30)
        } catch (e: Exception) {
            logger.info { "Failed to retrieve the magnet" }
            activity.runOnUiThread { printToast("Failed to fetch magnet info for $torrentName! error:$e") }
            // TODO: Implement for lower API levels.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                onTorrentDownloadFailure(torrentName, magnetInfoHash, peer)
            } else {
                throw NotImplementedError("Not implemented for Android versions below Nougat")
            }
            return
        }

        if (data != null) {

            val torrentInfo = TorrentInfo.bdecode(data)
            sessionActive = true
            signal = CountDownLatch(1)

            sessionManager.download(torrentInfo, appDirectory)
            activity.runOnUiThread { printToast("Managed to fetch torrent info for $torrentName, trying to download it via torrent!") }
            signal.await(1, TimeUnit.MINUTES)

            if (signal.count.toInt() == 1) {
                activity.runOnUiThread { printToast("Attempt to download timed out for $torrentName!") }
                signal = CountDownLatch(0)
                sessionManager.find(torrentInfo.infoHash())?.let { torrentHandle ->
                    sessionManager.remove(torrentHandle)
                }
                onTorrentDownloadFailure(torrentName, magnetInfoHash, peer)
            } else {
                onDownloadSuccess(magnetName)
            }
            sessionActive = false
        } else {
            logger.info { "Failed to retrieve the magnet" }
            activity.runOnUiThread { printToast("Failed to retrieve magnet for $torrentName!") }
            onTorrentDownloadFailure(torrentName, magnetInfoHash, peer)
        }
    }

    private fun onDownloadSuccess(torrentName: String) {
        activity.runOnUiThread {
            activity.createTorrent(torrentName)?.let {
                torrentInfos.add(it)
            }
        }
        downloadHasPassed()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun onTorrentDownloadFailure(
        torrentName: String,
        magnetInfoHash: String,
        peer: Peer
    ) {
        downloadHasFailed()
        if (literatureCommunity.evaProtocolEnabled && !evaDownload.activeDownload) {
            if (failedTorrents.containsKey(torrentName))
                failedTorrents[torrentName] = failedTorrents[torrentName]!!.plus(1)
            else
                failedTorrents[torrentName] = 1
            if (failedTorrents[torrentName] == TORRENT_ATTEMPTS_THRESHOLD)
                literatureCommunity.let {
                    activity.runOnUiThread {
                        downloadHasStarted(torrentName)
                        printToast("Torrent download failure threshold reached, attempting to fetch $torrentName through EVA Protocol!")
                    }
                    // TODO: Fix for lower Android API levels.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        literatureCommunity.sendLiteratureRequest(magnetInfoHash, peer)
                    } else {
                        throw NotImplementedError("This version of Android is not supported")
                    }
                    evaDownload =
                        EvaDownload(
                            true,
                            System.currentTimeMillis(),
                            magnetInfoHash,
                            peer,
                            0,
                            torrentName
                        )
                }
            else
                activity.runOnUiThread { printToast("$torrentName download failed ${failedTorrents[torrentName]} times") }
        }
    }

    fun printToast(s: String) {
        if (toastingEnabled)
            Toast.makeText(activity.applicationContext, s, Toast.LENGTH_LONG).show()
    }

    fun addDownloadToQueue(downloadsInProgressCount: Int) {
        downloadsInProgress = downloadsInProgressCount
        activity.runOnUiThread {
            printToast("Added $downloadsInProgressCount torrents to the queue")
        }
    }

    fun updateProgress(progress: Int) {
        activity.runOnUiThread {
            printToast("Download progress: $progress%")
        }
    }

    fun downloadHasStarted(torrentName: String) {
        activity.runOnUiThread {
            printToast("Downloading $torrentName")
        }
    }

    fun downloadHasFailed() {
        downloadsInProgress = kotlin.math.max(0, downloadsInProgress - 1)
        activity.runOnUiThread {
            printToast("Download failed, removing from queue")
        }
        updateProgress(0)
    }

    fun downloadHasPassed() {
        downloadsInProgress = kotlin.math.max(0, downloadsInProgress - 1)
        activity.runOnUiThread {
            printToast("Download passed")
        }
        updateProgress(0)
    }

    fun initialUISettings() {
        activity.runOnUiThread {
            // Update UI for downloading
            logger.info { "litdao: Initialize UI texts" }
        }
    }
}
