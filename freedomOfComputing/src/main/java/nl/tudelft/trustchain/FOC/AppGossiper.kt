package nl.tudelft.trustchain.FOC

import android.util.Log
import android.widget.Toast
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert
import kotlinx.android.synthetic.main.activity_main_foc.*
import kotlinx.android.synthetic.main.fragment_download.*
import kotlinx.coroutines.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.DemoCommunity
import nl.tudelft.trustchain.common.MyMessage
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

@Suppress("deprecation")
class AppGossiper(
    private val sessionManager: SessionManager,
    private val activity: MainActivityFOC
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val maxTorrentThreads = 5
    private val torrentHandles = ArrayList<TorrentHandle>()
    private val torrentInfos = ArrayList<TorrentInfo>()
    private val failedTorrents = HashMap<String, Long>()
    private var signal = CountDownLatch(0)
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
        val sp = SettingsPack()
        sp.seedingOutgoingConnections(true)
        val params =
            SessionParams(sp)
        sessionManager.start(params)
        activity.download_count.text = activity.getString(R.string.downloadsInProgress, downloadsInProgress)
        activity.inQueue.text = activity.getString(R.string.downloadsInQueue, kotlin.math.max(0, downloadsInProgress - 1))
        scope.launch {
            try {
                iterativelyShareApps()
            } catch (e: Exception) {
                activity.runOnUiThread { printToast(e.toString()) }
            }
        }
        scope.launch {
            try {
                iterativelyDownloadApps()
            } catch (e: Exception) {
                activity.runOnUiThread { printToast(e.toString()) }
            }
        }
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
                        Log.i("personal", "Torrent added")
                        (alert as AddTorrentAlert).handle().resume()
                    }
                    AlertType.BLOCK_FINISHED -> {
                        val a = alert as BlockFinishedAlert
                        val p = (a.handle().status().progress() * 100).toInt()
                        Log.i(
                            "personal",
                            "Progress: " + p + " for torrent name: " + a.torrentName()
                        )
                        Log.i("personal", java.lang.Long.toString(sessionManager.stats().totalDownload()))
                    }
                    AlertType.TORRENT_FINISHED -> {
                        signal.countDown()
                        Log.i("personal", "Torrent finished")
                        printToast("Torrent downloaded!!")
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
        val demoCommunity = IPv8Android.getInstance().getOverlay<DemoCommunity>()
        while (scope.isActive) {
            if (demoCommunity != null) {
                randomlyShareFiles(demoCommunity)
            }
            delay(GOSSIP_DELAY)
        }
    }

    private suspend fun iterativelyDownloadApps() {
        IPv8Android.getInstance().getOverlay<DemoCommunity>()?.let { demoCommunity ->
            while (scope.isActive) {
                downloadsInProgress = demoCommunity.getTorrentMessages().size
                activity.download_count.text = activity.getString(R.string.downloadsInProgress, downloadsInProgress)
                activity.inQueue.text = activity.getString(R.string.downloadsInQueue, kotlin.math.max(0, downloadsInProgress - 1))
                for (packet in ArrayList(demoCommunity.getTorrentMessages())) {
                    val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
                    Log.i("personal", peer.mid + ": " + payload.message)
                    val torrentName = payload.message.substringAfter("&dn=")
                        .substringBefore('&')
                    val magnetLink = payload.message.substringAfter("FOC:")
                    val torrentHash = magnetLink.substringAfter("magnet:?xt=urn:btih:")
                        .substringBefore("&dn=")
                    if (torrentInfos.none { it.infoHash().toString() == torrentHash }) {
                        if (failedTorrents.containsKey(torrentName)) {
                            // Wait at least 1000 seconds if torrent failed before
                            if ((System.currentTimeMillis() - failedTorrents[torrentName]!!) / 1000 < 1000)
                                continue
                        }
                        getMagnetLink(magnetLink, torrentName)
                    }
                }
            }
            delay(DOWNLOAD_DELAY)
        }
    }

    private fun randomlyShareFiles(demoCommunity: DemoCommunity) {
        populateKnownTorrents()
        torrentInfos.shuffle()
        val toSeed: ArrayList<TorrentInfo> = ArrayList(torrentInfos.take(maxTorrentThreads))
        torrentHandles.forEach { torrentHandle ->
            if (toSeed.any { it.infoHash() == torrentHandle.infoHash() }) {
                val dup = toSeed.find { it.infoHash() == torrentHandle.infoHash() }
                toSeed.remove(dup)
                if (dup != null) {
                    val magnet_link = "magnet:?xt=urn:btih:" + dup.infoHash() + "&dn=" + dup.name()
                    demoCommunity.informAboutTorrent(magnet_link)
                }
            } else
                torrentHandle.pause()
        }
        toSeed.forEach {
            downloadAndSeed(it, demoCommunity)
        }
    }

    private fun populateKnownTorrents() {
        activity.applicationContext.cacheDir.listFiles()?.forEachIndexed { _, file ->
            if (file.name.endsWith(".torrent")) {
                TorrentInfo(file).let { torrentInfo ->
                    if (torrentInfo.isValid) {
                        if (isTorrentOkay(torrentInfo, activity.applicationContext.cacheDir)) {
                            if (!torrentInfos.any { it.infoHash() == torrentInfo.infoHash() })
                                torrentInfos.add(torrentInfo)
                        }
                    }
                }
            }
        }
    }

    private fun downloadAndSeed(torrentInfo: TorrentInfo, demoCommunity: DemoCommunity) {
        if (torrentInfo.isValid) {
            sessionManager.download(torrentInfo, activity.applicationContext.cacheDir)
            sessionManager.find(torrentInfo.infoHash())?.let { torrentHandle ->
                torrentHandle.setFlags(torrentHandle.flags().and_(TorrentFlags.SEED_MODE))
                torrentHandle.pause()
                torrentHandle.resume()
                // This is a fix/hack that forces SEED_MODE to be available, for
                // an unsolved issue: seeding local torrents often result in an endless "CHECKING_FILES"
                // state
                val magnet_link = "magnet:?xt=urn:btih:" + torrentInfo.infoHash() + "&dn=" + torrentInfo.name()
                demoCommunity.informAboutTorrent(magnet_link)
                torrentHandles.add(torrentHandle)
            }
        }
    }

    fun isTorrentOkay(torrentInfo: TorrentInfo, saveDirectory: File): Boolean {
        File(saveDirectory.path + "/" + torrentInfo.name()).run {
            if (!(extension == "apk" || extension == "jar")) return false
            if (length() >= torrentInfo.totalSize()) return true
        }
        return false
    }

    @Suppress("deprecation")
    fun getMagnetLink(magnetLink: String, torrentName: String) {
        // Handling of the case where the user is already downloading the
        // same or another torrent
        activity.runOnUiThread {
            printToast("Found new torrent and start download")
            activity.currentDownload.text = activity.getString(R.string.currentTorrentDownload, "Downloading: $torrentName")
        }

        if (sessionActive || !magnetLink.startsWith("magnet:"))
            return

        val startIndexName = magnetLink.indexOf("&dn=")
        val stopIndexName =
            if (magnetLink.contains("&tr=")) magnetLink.indexOf("&tr") else magnetLink.length

        val magnetNameRaw = magnetLink.substring(startIndexName + 4, stopIndexName)
        Log.i("personal", magnetNameRaw)
        val magnetName = magnetNameRaw.replace('+', ' ', false)
        Log.i("personal", magnetName)
        activity.runOnUiThread { printToast(magnetName) }

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
                        Log.i("personal", "DHT contains $nodes nodes")
                        // signal.countDown();
                        timer.cancel()
                    }
                }
            },
            0, 1000
        )


        Log.i("personal", "Fetching the magnet uri, please wait...")
        val data: ByteArray
        try {
            data = sessionManager.fetchMagnet(magnetLink, 30)
        } catch (e: Exception) {
            Log.i("personal", "Failed to retrieve the magnet")
            activity.runOnUiThread {
                printToast("Failed to fetch magnet info for $torrentName!")
                printToast(e.toString())
                activity.createUnsuccessfulTorrentButton(torrentName)
                activity.download_count.text = activity.getString(R.string.downloadsInProgress, --downloadsInProgress)
                activity.inQueue.text = activity.getString(R.string.downloadsInQueue, kotlin.math.max(0, downloadsInProgress - 1))
            }
            failedTorrents[torrentName] = System.currentTimeMillis()
            return
        }

        if (data != null) {
            val torrentInfoAsString = Entry.bdecode(data).toString()
            Log.i("personal", torrentInfoAsString)

            val torrentInfo = TorrentInfo.bdecode(data)
            sessionActive = true
            signal = CountDownLatch(1)

            sessionManager.download(torrentInfo, activity.applicationContext.cacheDir)

            // TODO: When calling showAllFiles() the buttons that are created for unsuccessful downloads will disappear
            activity.runOnUiThread { activity.showAllFiles() }
            signal.await(3, TimeUnit.MINUTES)
            if (signal.count.toInt() == 1) {
                activity.runOnUiThread { printToast("Attempt to download timed out for $torrentName!") }
                signal = CountDownLatch(0)
                failedTorrents[torrentName] = System.currentTimeMillis()
            }
            sessionActive = false
            activity.createTorrent(magnetName)
            torrentInfos.add(torrentInfo)
        } else {
            Log.i("personal", "Failed to retrieve the magnet")
            activity.runOnUiThread {
                printToast("Failed to retrieve magnet for $torrentName!")
                activity.createUnsuccessfulTorrentButton(torrentName)
            }
            failedTorrents[torrentName] = System.currentTimeMillis()
        }
        activity.runOnUiThread {
            activity.download_count.text = activity.getString(R.string.downloadsInProgress, --downloadsInProgress)
            activity.currentDownload.text = activity.getString(R.string.currentTorrentDownload, "No download in progress...")
            activity.inQueue.text = activity.getString(R.string.downloadsInQueue, kotlin.math.max(0, downloadsInProgress - 1))
        }
    }

    fun printToast(s: String) {
        Toast.makeText(activity.applicationContext, s, Toast.LENGTH_LONG).show()
    }

}
