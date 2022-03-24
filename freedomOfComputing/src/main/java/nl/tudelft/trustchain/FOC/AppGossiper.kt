package nl.tudelft.trustchain.FOC

import android.util.Log
import android.widget.Toast
import com.frostwire.jlibtorrent.*
import kotlinx.android.synthetic.main.activity_main_foc.*
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
        val sp = SettingsPack()
        sp.seedingOutgoingConnections(true)
        val params =
            SessionParams(sp)
        sessionManager.start(params)
        activity.download_count.text = activity.getString(R.string.downloadsInProgress, downloadsInProgress)
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

    /**
     * This is a very simplistic way to crawl all chains from the peers you know
     */
    private suspend fun iterativelyShareApps() {
        val demoCommunity = IPv8Android.getInstance().getOverlay<DemoCommunity>()
        while (scope.isActive) {
            if (demoCommunity != null) {
                randomlyShareFiles(demoCommunity)
            }
            delay(10000)
        }
    }

    private suspend fun iterativelyDownloadApps() {
        IPv8Android.getInstance().getOverlay<DemoCommunity>()?.let { demoCommunity ->
            while (scope.isActive) {
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
            delay(20000)
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
        activity.runOnUiThread { printToast("Found new torrent and start download") }

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
            activity.runOnUiThread { printToast("Failed to fetch magnet info for $torrentName!") }
            activity.runOnUiThread { printToast(e.toString()) }
            failedTorrents[torrentName] = System.currentTimeMillis()
            return
        }

        if (data != null) {
            val torrentInfoAsString = Entry.bdecode(data).toString()
            Log.i("personal", torrentInfoAsString)

            val torrentInfo = TorrentInfo.bdecode(data)
            sessionActive = true
            activity.signal = CountDownLatch(1)

            downloadsInProgress += 1
            sessionManager.download(torrentInfo, activity.applicationContext.cacheDir)
            downloadsInProgress -= 1

            activity.runOnUiThread { activity.showAllFiles() }
            activity.signal.await(3, TimeUnit.MINUTES)
            if (activity.signal.count.toInt() == 1) {
                activity.runOnUiThread { printToast("Attempt to download timed out for $torrentName!") }
                activity.signal = CountDownLatch(0)
                failedTorrents[torrentName] = System.currentTimeMillis()
            }
            sessionActive = false
            activity.createTorrent(magnetName)
            torrentInfos.add(torrentInfo)
        } else {
            Log.i("personal", "Failed to retrieve the magnet")
            activity.runOnUiThread { printToast("Failed to retrieve magnet for $torrentName!") }
            failedTorrents[torrentName] = System.currentTimeMillis()
        }
    }

    fun printToast(s: String) {
        Toast.makeText(activity.applicationContext, s, Toast.LENGTH_LONG).show()
    }

}
