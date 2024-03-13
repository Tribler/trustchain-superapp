package nl.tudelft.trustchain.foc

import android.widget.Toast
import com.frostwire.jlibtorrent.AlertListener
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.SessionParams
import com.frostwire.jlibtorrent.SettingsPack
import com.frostwire.jlibtorrent.TorrentFlags
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.eva.PeerBusyException
import nl.tudelft.ipv8.messaging.eva.TimeoutException
import nl.tudelft.ipv8.messaging.eva.TransferType
import nl.tudelft.trustchain.common.freedomOfComputing.AppPayload
import nl.tudelft.trustchain.foc.community.FOCCommunityBase
import nl.tudelft.trustchain.foc.community.FOCMessage
import nl.tudelft.trustchain.foc.util.ExtensionUtils.Companion.APK_EXTENSION
import nl.tudelft.trustchain.foc.util.ExtensionUtils.Companion.TORRENT_EXTENSION
import nl.tudelft.trustchain.foc.util.ExtensionUtils.Companion.supportedAppExtensions
import nl.tudelft.trustchain.foc.util.MagnetUtils.Companion.ADDRESS_TRACKER
import nl.tudelft.trustchain.foc.util.MagnetUtils.Companion.ADDRESS_TRACKER_APPENDER
import nl.tudelft.trustchain.foc.util.MagnetUtils.Companion.DISPLAY_NAME_APPENDER
import nl.tudelft.trustchain.foc.util.MagnetUtils.Companion.MAGNET_HEADER_STRING
import nl.tudelft.trustchain.foc.util.MagnetUtils.Companion.PRE_HASH_STRING
import nl.tudelft.trustchain.foc.util.MagnetUtils.Companion.constructMagnetLink
import java.io.File
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * This gossips data about 5 random apps with peers on demoCommunity every 10 seconds and fetches new apps from peers every 20 seconds
 */

lateinit var appGossiperInstance: AppGossiper
const val GOSSIP_DELAY: Long = 10000
const val DOWNLOAD_DELAY: Long = 20000

const val TORRENT_ATTEMPTS_THRESHOLD = 1
const val EVA_RETRIES = 10
private val logger = KotlinLogging.logger {}

class AppGossiper(
    private val sessionManager: SessionManager,
    private val activity: MainActivityFOC,
    private val focCommunity: FOCCommunityBase,
    private val toastingEnabled: Boolean
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var evaDownload: EvaDownload = EvaDownload()
    private val maxTorrentThreads = 5
    private val torrentHandles = ArrayList<TorrentHandle>()
    private val torrentInfos = ArrayList<TorrentInfo>()
    val failedTorrents = HashMap<String, Int>()
    internal var signal = CountDownLatch(0)
    private val appDirectory = activity.applicationContext.cacheDir
    private var gossipingPaused = false
    private var downloadingPaused = false
    var sessionActive = false
    var downloadsInProgress = 0
    var evaRetries = 0
    var currentDownloadInProgress = activity.getString(R.string.noDownloadInProgress)

    companion object {
        fun getInstance(
            sessionManager: SessionManager,
            activity: MainActivityFOC,
            focCommunity: FOCCommunityBase,
            toastingEnabled: Boolean = true
        ): AppGossiper {
            if (!::appGossiperInstance.isInitialized) {
                appGossiperInstance =
                    AppGossiper(
                        sessionManager,
                        activity,
                        focCommunity,
                        toastingEnabled
                    )
            }
            return appGossiperInstance
        }
    }

    fun start() {
        printToast("Start gossiper")
        // This call seems redundant, but it's necessary to populate known torrents
        // before downloading starts so we don't retry known downloads
        populateKnownTorrents()
        initializeTorrentSession()
        initializeEvaCallbacks()
        initialUISettings()
        val sp = SettingsPack()
        sp.seedingOutgoingConnections(true)
        val params =
            SessionParams(sp)
        sessionManager.start(params)
        scope.launch {
            iterativelyShareApps()
        }
        scope.launch {
            iterativelyDownloadApps()
        }
    }

    private fun initializeEvaCallbacks() {
        focCommunity.setEVAOnReceiveCompleteCallback { peer, _, _, data ->
            data?.let {
                val packet = Packet(peer.address, data)
                val (_, payload) =
                    packet.getDecryptedAuthPayload(
                        AppPayload.Deserializer,
                        focCommunity.myPeer.key as PrivateKey
                    )
                evaDownload.activeDownload = false
                activity.runOnUiThread {
                    printToast("Torrent ${payload.appName} fetched through EVA protocol!")
                    onDownloadSuccess(payload.appName)
                }
            }
        }

        focCommunity.setEVAOnReceiveProgressCallback { _, _, progress ->
            updateProgress(progress = progress.progress.toInt())
        }

        focCommunity.setEVAOnErrorCallback { _, exception ->
            if (evaDownload.activeDownload && exception.transfer?.type == TransferType.INCOMING) {
                if (exception is TimeoutException || exception is PeerBusyException) {
                    activity.runOnUiThread { printToast("Failed to fetch through EVA protocol because $exception! Retrying") }
                    retryActiveEvaDownload()
                } else {
                    activity.runOnUiThread {
                        printToast(
                            "Can't fetch through EVA because of $exception will continue to retry via torrent"
                        )
                    }
                    evaDownload.activeDownload = false
                    downloadHasFailed()
                }
            }
        }
    }

    private fun retryActiveEvaDownload() {
        if (evaDownload.retryAttempts < EVA_RETRIES) {
            evaDownload.peer?.let {
                focCommunity.sendAppRequest(
                    evaDownload.magnetInfoHash,
                    it,
                    evaDownload.attemptUUID!!
                )
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
                evaRetries = evaDownload.retryAttempts
                activity.binding.debugLayout.evaRetryCounter.text =
                    activity.getString(R.string.evaRetries, evaRetries)
            }
        }
    }

    fun pause() {
        gossipingPaused = true
        downloadingPaused = true
    }

    fun resume() {
        gossipingPaused = false
        downloadingPaused = false
    }

    private fun initializeTorrentSession() {
        sessionManager.addListener(
            object : AlertListener {
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
            }
        )
    }

    /**
     * This is a very simplistic way to crawl all chains from the peers you know
     */
    private suspend fun iterativelyShareApps() {
        while (scope.isActive) {
            if (!gossipingPaused) {
                try {
                    randomlyShareFiles()
                } catch (e: Exception) {
                    activity.runOnUiThread { printToast(e.toString()) }
                }
            }
            delay(GOSSIP_DELAY)
        }
    }

    private suspend fun iterativelyDownloadApps() {
        while (scope.isActive) {
            if (!downloadingPaused) {
                try {
                    downloadPendingFiles()
                    if (evaDownload.activeDownload && evaDownload.lastRequest?.let { it ->
                            System.currentTimeMillis() - it
                        } ?: 0 > 30 * 1000
                    ) {
                        activity.runOnUiThread { printToast("EVA Protocol timed out, retrying") }
                        retryActiveEvaDownload()
                    }
                } catch (e: Exception) {
                    activity.runOnUiThread { printToast(e.toString()) }
                }
            }
            delay(DOWNLOAD_DELAY)
        }
    }

    fun addButtonsInAdvance(packets: ArrayList<Pair<Peer, FOCMessage>>) {
        for (packet in packets) {
            val peer = packet.first
            val payload = packet.second
            logger.info { peer.mid + ": " + payload.message }
            val torrentName =
                payload.message.substringAfter(DISPLAY_NAME_APPENDER)
                    .substringBefore('&')
            activity.runOnUiThread {
                val existingButton = activity.torrentMap.entries.find { entry -> entry.key.text == torrentName }?.key
                if (existingButton == null) {
                    addDownloadToQueue()
                    activity.createUnsuccessfulTorrentButton(torrentName)
                }
            }
        }
    }

    private fun downloadPendingFiles() {
        addButtonsInAdvance(focCommunity.torrentMessagesList)
        for ((peer, payload) in ArrayList(focCommunity.torrentMessagesList)) {
            val torrentName =
                payload.message.substringAfter(DISPLAY_NAME_APPENDER)
                    .substringBefore('&')
            val magnetLink = payload.message.substringAfter("foc:")
            val torrentHash =
                magnetLink.substringAfter(PRE_HASH_STRING)
                    .substringBefore(DISPLAY_NAME_APPENDER)
            if (torrentInfos.none { it.infoHash().toString() == torrentHash }) {
                if (failedTorrents.containsKey(torrentName)) {
                    // Wait at least 1000 seconds if torrent failed before
                    if (failedTorrents[torrentName]!! >= TORRENT_ATTEMPTS_THRESHOLD) {
                        continue
                    }
                }
                getMagnetLink(magnetLink, torrentName, peer)
            }
        }
    }

    private fun randomlyShareFiles() {
        focCommunity.run {
            populateKnownTorrents()
            torrentInfos.shuffle()
            val toSeed: ArrayList<TorrentInfo> = ArrayList(torrentInfos.take(maxTorrentThreads))
            torrentHandles.forEach { torrentHandle ->
                if (toSeed.any { it.infoHash() == torrentHandle.infoHash() }) {
                    val dup = toSeed.find { it.infoHash() == torrentHandle.infoHash() }
                    toSeed.remove(dup)
                    if (dup != null) {
                        val magnetLink = constructMagnetLink(dup.infoHash(), dup.name())
                        informAboutTorrent(magnetLink)
                    }
                } else {
                    if (torrentHandle.isValid) {
                        torrentHandle.pause()
                    }
                }
            }
            toSeed.forEach {
                downloadAndSeed(it)
            }
        }
    }

    private fun populateKnownTorrents() {
        // Ensure the apk file is read only see:
        // https://developer.android.com/about/versions/14/behavior-changes-14#safer-dynamic-code-loading
        appDirectory.listFiles()?.forEachIndexed { _, file ->
            if (file.name.endsWith(APK_EXTENSION)) {
                file.setReadOnly()
            }
            if (file.name.endsWith(TORRENT_EXTENSION)) {
                TorrentInfo(file).let { torrentInfo ->
                    if (torrentInfo.isValid) {
                        if (isTorrentOkay(torrentInfo, appDirectory)) {
                            if (!torrentInfos.any { it.infoHash() == torrentInfo.infoHash() }) {
                                torrentInfos.add(torrentInfo)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun downloadAndSeed(torrentInfo: TorrentInfo) {
        if (torrentInfo.isValid) {
            sessionManager.download(torrentInfo, appDirectory)
            sessionManager.find(torrentInfo.infoHash())?.let { torrentHandle ->
                if (torrentHandle.isValid) {
                    torrentHandle.setFlags(torrentHandle.flags().and_(TorrentFlags.SEED_MODE))
                    torrentHandle.pause()
                    torrentHandle.resume()
                    // This is a fix/hack that forces SEED_MODE to be available, for
                    // an unsolved issue: seeding local torrents often result in an endless "CHECKING_FILES"
                    // state
                    val magnetLink = constructMagnetLink(torrentInfo.infoHash(), torrentInfo.name())
                    focCommunity.informAboutTorrent(magnetLink)
                    torrentHandles.add(torrentHandle)
                }
            }
        }
    }

    private fun isTorrentOkay(
        torrentInfo: TorrentInfo,
        saveDirectory: File
    ): Boolean {
        File(saveDirectory.path + "/" + torrentInfo.name()).run {
            if (!supportedAppExtensions.contains(extension)) return false
            if (length() >= torrentInfo.totalSize()) return true
        }
        return false
    }

    fun getMagnetLink(
        magnetLink: String,
        torrentName: String,
        peer: Peer
    ) {
        // Handling of the case where the user is already downloading the
        // same or another torrent

        if (sessionActive || !magnetLink.startsWith(MAGNET_HEADER_STRING) || evaDownload.activeDownload) {
            return
        }

        downloadHasStarted(torrentName)

        activity.runOnUiThread {
            printToast("Found new torrent $torrentName attempting to download!")
        }
        val startIndexName = magnetLink.indexOf(DISPLAY_NAME_APPENDER)
        val stopIndexName =
            if (magnetLink.contains(ADDRESS_TRACKER_APPENDER)) magnetLink.indexOf(ADDRESS_TRACKER) else magnetLink.length

        val magnetNameRaw = magnetLink.substring(startIndexName + 4, stopIndexName)
        logger.info { magnetNameRaw }
        val magnetName = magnetNameRaw.replace('+', ' ', false)
        val magnetInfoHash = magnetLink.substring(PRE_HASH_STRING.length, startIndexName)
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
                        // signal.countDown();
                        timer.cancel()
                    }
                }
            },
            0,
            1000
        )

        logger.info { "Fetching the magnet uri, please wait..." }
        val data: ByteArray
        try {
            data = sessionManager.fetchMagnet(magnetLink, 30)
        } catch (e: Exception) {
            logger.info { "Failed to retrieve the magnet" }
            activity.runOnUiThread { printToast("Failed to fetch magnet info for $torrentName! error:$e") }
            onTorrentDownloadFailure(torrentName, magnetInfoHash, peer)
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
        // Ensure the apk file is read only see:
        // https://developer.android.com/about/versions/14/behavior-changes-14#safer-dynamic-code-loading
        appDirectory.listFiles { _, file ->
            file.contains(torrentName) && file.endsWith(APK_EXTENSION)
        }?.get(0)?.setReadOnly()
        activity.runOnUiThread {
            activity.createTorrent(torrentName)?.let {
                torrentInfos.add(it)
            }
            activity.showAddedFile(torrentName)
        }
        downloadHasPassed()
    }

    private fun onTorrentDownloadFailure(
        torrentName: String,
        magnetInfoHash: String,
        peer: Peer
    ) {
        downloadHasFailed()
        if (focCommunity.evaProtocolEnabled && !evaDownload.activeDownload) {
            if (failedTorrents.containsKey(torrentName)) {
                failedTorrents[torrentName] = failedTorrents[torrentName]!!.plus(1)
            } else {
                failedTorrents[torrentName] = 1
            }
            if (failedTorrents[torrentName] == TORRENT_ATTEMPTS_THRESHOLD) {
                focCommunity.let {
                    activity.runOnUiThread {
                        downloadHasStarted(torrentName)
                        printToast("Torrent download failure threshold reached, attempting to fetch $torrentName through EVA Protocol!")
                    }
                    val attemptUUID = UUID.randomUUID().toString()
                    focCommunity.sendAppRequest(magnetInfoHash, peer, attemptUUID)
                    evaDownload =
                        EvaDownload(
                            true,
                            System.currentTimeMillis(),
                            magnetInfoHash,
                            peer,
                            0,
                            torrentName,
                            attemptUUID
                        )
                }
            } else {
                activity.runOnUiThread { printToast("$torrentName download failed ${failedTorrents[torrentName]} times") }
            }
        }
    }

    fun removeTorrent(torrentName: String) {
        val torrentInfo: TorrentInfo? =
            this.torrentInfos.find { torrentInfo -> torrentInfo.name() == torrentName }
        if (torrentInfo != null) {
            this.torrentInfos.remove(torrentInfo)
        }
        if (failedTorrents.containsKey(torrentName)) {
            failedTorrents.remove(torrentName)
        }
    }

    fun printToast(s: String) {
        if (toastingEnabled) {
            Toast.makeText(activity.applicationContext, s, Toast.LENGTH_LONG).show()
        }
    }

    private fun addDownloadToQueue() {
        activity.runOnUiThread {
            val downloadCount = activity.binding.downloadCount
            downloadCount.text =
                activity.getString(R.string.downloadsInProgress, ++downloadsInProgress)

            val inQueue = activity.binding.popUpLayout.inQueue
            inQueue.text =
                activity.getString(
                    R.string.downloadsInQueue,
                    kotlin.math.max(0, downloadsInProgress - 1)
                )
        }
    }

    fun updateProgress(progress: Int) {
        activity.runOnUiThread {
            val progressBar = activity.binding.popUpLayout.progressBar
            progressBar.progress = progress

            val progressBarPercentage = activity.binding.popUpLayout.progressBarPercentage
            progressBarPercentage.text =
                activity.getString(R.string.downloadProgressPercentage, "$progress%")
        }
    }

    private fun downloadHasStarted(torrentName: String) {
        currentDownloadInProgress = activity.getString(R.string.currentTorrentDownload, torrentName)
        activity.runOnUiThread {
            val inQueue = activity.binding.popUpLayout.inQueue
            inQueue.text =
                activity.getString(
                    R.string.downloadsInQueue,
                    kotlin.math.max(0, downloadsInProgress - 1)
                )
            val currentDownload = activity.binding.popUpLayout.currentDownload
            currentDownload.text = currentDownloadInProgress
        }
    }

    private fun downloadHasFailed() {
        currentDownloadInProgress = activity.getString(R.string.noDownloadInProgress)
        activity.runOnUiThread {
            val currentDownload = activity.binding.popUpLayout.currentDownload
            currentDownload.text = currentDownloadInProgress
            val failedCounter = activity.binding.debugLayout.failedCounter
            failedCounter.text =
                activity.getString(R.string.failedCounter, failedTorrents.toString())
        }
        updateProgress(0)
    }

    private fun downloadHasPassed() {
        currentDownloadInProgress = activity.getString(R.string.noDownloadInProgress)
        activity.runOnUiThread {
            activity.binding.downloadCount.text =
                activity.getString(R.string.downloadsInProgress, --downloadsInProgress)
            val inQueue = activity.binding.popUpLayout.inQueue
            inQueue.text =
                activity.getString(
                    R.string.downloadsInQueue,
                    kotlin.math.max(0, downloadsInProgress - 1)
                )

            val currentDownload = activity.binding.popUpLayout.currentDownload
            currentDownload.text = currentDownloadInProgress
        }
        updateProgress(0)
    }

    private fun initialUISettings() {
        activity.runOnUiThread {
            activity.binding.downloadCount.text =
                activity.getString(R.string.downloadsInProgress, 0)

            val inQueue = activity.binding.popUpLayout.inQueue
            inQueue.text =
                activity.getString(
                    R.string.downloadsInQueue,
                    kotlin.math.max(0, downloadsInProgress - 1)
                )

            val currentDownload = activity.binding.popUpLayout.currentDownload
            currentDownload.text = currentDownloadInProgress

            val progressBarPercentage = activity.binding.popUpLayout.progressBarPercentage
            progressBarPercentage.text =
                activity.getString(R.string.downloadProgressPercentage, "0%")

            val evaRetryCounter = activity.binding.debugLayout.evaRetryCounter
            evaRetryCounter.text =
                activity.getString(R.string.evaRetries, evaRetries)

            val failedCounter = activity.binding.debugLayout.failedCounter
            failedCounter.text =
                activity.getString(R.string.failedCounter, failedTorrents.toString())
        }
    }
}
