package nl.tudelft.trustchain.FOC

import android.util.Log
import android.widget.Toast
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert
import kotlinx.android.synthetic.main.activity_main_foc.*
import kotlinx.android.synthetic.main.fragment_debugging.*
import kotlinx.android.synthetic.main.fragment_download.*
import kotlinx.coroutines.*
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.eva.PeerBusyException
import nl.tudelft.ipv8.messaging.eva.TimeoutException
import nl.tudelft.ipv8.messaging.eva.TransferType
import nl.tudelft.trustchain.FOC.util.ExtensionUtils.Companion.supportedAppExtensions
import nl.tudelft.trustchain.FOC.util.ExtensionUtils.Companion.torrentExtension
import nl.tudelft.trustchain.FOC.util.MagnetUtils.Companion.addressTracker
import nl.tudelft.trustchain.FOC.util.MagnetUtils.Companion.addressTrackerAppender
import nl.tudelft.trustchain.FOC.util.MagnetUtils.Companion.constructMagnetLink
import nl.tudelft.trustchain.FOC.util.MagnetUtils.Companion.displayNameAppender
import nl.tudelft.trustchain.FOC.util.MagnetUtils.Companion.magnetHeaderString
import nl.tudelft.trustchain.FOC.util.MagnetUtils.Companion.preHashString
import nl.tudelft.trustchain.common.DemoCommunity
import nl.tudelft.trustchain.common.MyMessage
import nl.tudelft.trustchain.common.freedomOfComputing.AppPayload
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

/**
 * This gossips data about 5 random apps with peers on demoCommunity every 10 seconds and fetches new apps from peers every 20 seconds
 */

lateinit var appGossiperInstance: AppGossiper
const val GOSSIP_DELAY: Long = 10000
const val DOWNLOAD_DELAY: Long = 20000

const val TORRENT_ATTEMPTS_THRESHOLD = 1
const val EVA_RETRIES = 10

@Suppress("deprecation")
class AppGossiper(
    private val sessionManager: SessionManager,
    private val activity: MainActivityFOC
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val maxTorrentThreads = 5
    private val torrentHandles = ArrayList<TorrentHandle>()
    private val torrentInfos = ArrayList<TorrentInfo>()
    private val failedTorrents = HashMap<String, Int>()
    private var signal = CountDownLatch(0)
    private val appDirectory = activity.applicationContext.cacheDir
    private val demoCommunity = IPv8Android.getInstance().getOverlay<DemoCommunity>()
    private var gossipingPaused = false
    private var downloadingPaused = false
    private var evaDownload: EvaDownload = EvaDownload()
    var sessionActive = false
    var downloadsInProgress = 0

    companion object {
        fun getInstance(
            sessionManager: SessionManager,
            activity: MainActivityFOC
        ): AppGossiper {
            if (!::appGossiperInstance.isInitialized) {
                appGossiperInstance = AppGossiper(sessionManager, activity)
            }
            return appGossiperInstance
        }
    }

    fun start() {
        printToast("Start gossiper")
        populateKnownTorrents()
        initializeTorrentSession()
        initializeEvaCallbacks()
        val sp = SettingsPack()
        sp.seedingOutgoingConnections(true)
        val params =
            SessionParams(sp)
        sessionManager.start(params)
        initialUISettings()
        scope.launch {
            iterativelyShareApps()
        }
        scope.launch {
            iterativelyDownloadApps()
        }
    }

    private fun initializeEvaCallbacks() {
        demoCommunity?.setEVAOnReceiveCompleteCallback { peer, _, _, data ->
            data?.let {
                val packet = Packet(peer.address, data)
                val (_, payload) = packet.getDecryptedAuthPayload(
                    AppPayload.Deserializer, demoCommunity.myPeer.key as PrivateKey
                )
                evaDownload.activeDownload = false
                activity.runOnUiThread {
                    printToast("Torrent ${payload.appName} fetched through EVA protocol!")
                    onDownloadSuccess(payload.appName)
                }
            }
        }

        demoCommunity?.setEVAOnReceiveProgressCallback { _, _, progress ->
            updateProgress(progress = progress.progress.toInt())
        }

        demoCommunity?.setEVAOnErrorCallback { _, exception ->
            downloadHasFailed()
            if (evaDownload.activeDownload && exception.transfer?.type == TransferType.INCOMING) {
                if (exception is TimeoutException || exception is PeerBusyException) {
                    activity.runOnUiThread { printToast("Failed to fetch through EVA protocol because $exception! Retrying") }
                    retryActiveEvaDownload()
                } else {
                    activity.runOnUiThread { printToast("Can't fetch through EVA because of $exception will continue to retry via torrent") }
                    evaDownload.activeDownload = false
                }
            }
        }
    }

    private fun retryActiveEvaDownload() {
        if (evaDownload.retryAttempts < EVA_RETRIES) {
            evaDownload.peer?.let { demoCommunity?.sendAppRequest(evaDownload.magnetInfoHash, it) }
            evaDownload.lastRequest = System.currentTimeMillis()
            evaDownload.retryAttempts++
            if (evaDownload.retryAttempts == EVA_RETRIES) {
                activity.runOnUiThread { printToast("Giving up on trying to download via EVA") }
                evaDownload.activeDownload = false
            }
            activity.runOnUiThread { activity.evaRetryCounter.text = activity.getString(R.string.evaRetries, evaDownload.retryAttempts) }
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
                val type = alert.type()

                when (type) {
                    AlertType.ADD_TORRENT -> {
                        Log.i("appGossiper", "Torrent added")
                        (alert as AddTorrentAlert).handle().resume()
                    }
                    AlertType.BLOCK_FINISHED -> {
                        val a = alert as BlockFinishedAlert
                        val p = (a.handle().status().progress() * 100).toInt()
                        Log.i(
                            "appGossiper",
                            "Progress: " + p + " for torrent name: " + a.torrentName()
                        )
                        printToast("Download in progress!")
                        updateProgress(p)
                        Log.i("appGossiper", java.lang.Long.toString(sessionManager.stats().totalDownload()))
                    }
                    AlertType.TORRENT_FINISHED -> {
                        signal.countDown()
                        Log.i("appGossiper", "Torrent finished")

                        activity.runOnUiThread {
                            printToast("Torrent downloaded!!")
                        }
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
                    if (evaDownload.activeDownload && evaDownload.lastRequest?.let { it -> System.currentTimeMillis() - it } ?: 0 > 30 * 1000) {
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

    private suspend fun downloadPendingFiles() {
        IPv8Android.getInstance().getOverlay<DemoCommunity>()?.let { demoCommunity ->
            for (packet in ArrayList(demoCommunity.getTorrentMessages())) {
                val (peer, payload) = packet.getAuthPayload(MyMessage)
                Log.i("appGossiper", peer.mid + ": " + payload.message)
                val torrentName = payload.message.substringAfter("&dn=")
                    .substringBefore('&')
                val magnetLink = payload.message.substringAfter("FOC:")
                val torrentHash = magnetLink.substringAfter("magnet:?xt=urn:btih:")
                    .substringBefore("&dn=")
                if (torrentInfos.none { it.infoHash().toString() == torrentHash }) {
                    if (failedTorrents.containsKey(torrentName)) {
                        // Wait at least 1000 seconds if torrent failed before
                        if (failedTorrents[torrentName]!! >= TORRENT_ATTEMPTS_THRESHOLD)
                            continue
                    }
                    activity.runOnUiThread { activity.download_count.text = activity.getString(R.string.downloadsInProgress, ++downloadsInProgress) }
                    getMagnetLink(magnetLink, torrentName, peer)
                }
            }
        }
    }

    private fun randomlyShareFiles() {
        demoCommunity?.run {
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
                demoCommunity?.informAboutTorrent(magnetLink)
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

    @Suppress("deprecation")
    fun getMagnetLink(magnetLink: String, torrentName: String, peer: Peer) {
        // Handling of the case where the user is already downloading the
        // same or another torrent
        activity.runOnUiThread {
            printToast("Found new torrent $torrentName attempting to download!")
        }
        downloadHasStarted(torrentName)

        if (sessionActive || !magnetLink.startsWith(magnetHeaderString) || evaDownload.activeDownload)
            return


        val startIndexName = magnetLink.indexOf(displayNameAppender)
        val stopIndexName =
            if (magnetLink.contains(addressTrackerAppender)) magnetLink.indexOf(addressTracker) else magnetLink.length

        val magnetNameRaw = magnetLink.substring(startIndexName + 4, stopIndexName)
        Log.i("appGossiper", magnetNameRaw)
        val magnetName = magnetNameRaw.replace('+', ' ', false)
        val magnetInfoHash = magnetLink.substring(preHashString.length, startIndexName)
        Log.i("appGossiper", magnetName)

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
                        Log.i("appGossiper", "DHT contains $nodes nodes")
                        // signal.countDown();
                        timer.cancel()
                    }
                }
            },
            0, 1000
        )


        Log.i("appGossiper", "Fetching the magnet uri, please wait...")
        val data: ByteArray
        try {
            data = sessionManager.fetchMagnet(magnetLink, 30)
        } catch (e: Exception) {
            Log.i("appGossiper", "Failed to retrieve the magnet")
            activity.runOnUiThread { printToast("Failed to fetch magnet info for $torrentName! error:${e}") }
            onTorrentDownloadFailure(torrentName, magnetInfoHash, peer)
            return
        }

        if (data != null) {
            val torrentInfoAsString = Entry.bdecode(data).toString()
            Log.i("appGossiper", torrentInfoAsString)

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
            Log.i("appGossiper", "Failed to retrieve the magnet")
            activity.runOnUiThread { printToast("Failed to retrieve magnet for $torrentName!") }
            onTorrentDownloadFailure(torrentName, magnetInfoHash, peer)
        }
    }

    private fun onDownloadSuccess(torrentName: String) {
        activity.runOnUiThread {
            activity.createTorrent(torrentName)?.let {
                torrentInfos.add(it)
            }
            activity.showAllFiles()
        }
        downloadHasPassed()
    }

    private fun onTorrentDownloadFailure(
        torrentName: String,
        magnetInfoHash: String,
        peer: Peer
    ) {
        downloadHasFailed()
        if (demoCommunity?.evaProtocolEnabled == true && !evaDownload.activeDownload) {
            if (failedTorrents.containsKey(torrentName))
                failedTorrents[torrentName] = failedTorrents[torrentName]!!.plus(1)
            else
                failedTorrents[torrentName] = 1
            if (failedTorrents[torrentName] == TORRENT_ATTEMPTS_THRESHOLD)
                demoCommunity.let {
                    activity.runOnUiThread { printToast("Torrent download failure threshold reached, attempting to fetch $torrentName through EVA Protocol!") }
                    demoCommunity.sendAppRequest(magnetInfoHash, peer)
                    evaDownload =
                        EvaDownload(true, System.currentTimeMillis(), magnetInfoHash, peer, 0)
                }
            else
                activity.runOnUiThread { printToast("$torrentName download failed ${failedTorrents[torrentName]} times") }

        }
    }

    fun printToast(s: String) {
        Toast.makeText(activity.applicationContext, s, Toast.LENGTH_LONG).show()
    }

    fun updateProgress(progress: Int) {
        activity.runOnUiThread {
            activity.progressBar.progress = progress
            activity.progressBarPercentage.text = activity.getString(R.string.downloadProgressPercentage, "$progress%")
        }
    }

    fun downloadHasStarted(torrentName: String) {
        activity.runOnUiThread {
            activity.inQueue.text = activity.getString(R.string.downloadsInQueue, kotlin.math.max(0, downloadsInProgress - 1))
            activity.currentDownload.text = activity.getString(R.string.currentTorrentDownload, "Downloading: $torrentName")
            activity.createUnsuccessfulTorrentButton(torrentName)
        }
    }

    fun downloadHasFailed() {
        activity.runOnUiThread {
            downloadsInProgress = kotlin.math.max(0, downloadsInProgress - 1)
            activity.download_count.text = activity.getString(R.string.downloadsInProgress, downloadsInProgress)
            activity.inQueue.text = activity.getString(R.string.downloadsInQueue, kotlin.math.max(0, downloadsInProgress - 1))
            activity.currentDownload.text = activity.getString(R.string.currentTorrentDownload, "No download in progress")
            activity.failedCounter.text = activity.getString(R.string.failedCounter, failedTorrents.toString())
            updateProgress(0)
        }
    }

    fun downloadHasPassed() {
        activity.runOnUiThread {
            downloadsInProgress = kotlin.math.max(0, downloadsInProgress - 1)
            activity.download_count.text = activity.getString(R.string.downloadsInProgress, downloadsInProgress)
            activity.inQueue.text = activity.getString(R.string.downloadsInQueue, kotlin.math.max(0, downloadsInProgress - 1))
            activity.currentDownload.text = activity.getString(R.string.currentTorrentDownload, "No download in progress")
            updateProgress(0)
        }
    }

    fun initialUISettings() {
        activity.runOnUiThread {
            activity.download_count.text = activity.getString(R.string.downloadsInProgress, 0)
            activity.inQueue.text = activity.getString(R.string.downloadsInQueue, kotlin.math.max(0, downloadsInProgress - 1))
            activity.currentDownload.text = activity.getString(R.string.currentTorrentDownload, "No download in progress")
            activity.progressBarPercentage.text = activity.getString(R.string.downloadProgressPercentage, "0%")
            activity.evaRetryCounter.text = activity.getString(R.string.evaRetries, 0)
            activity.failedCounter.text = activity.getString(R.string.failedCounter, "{}")
        }
    }
}
