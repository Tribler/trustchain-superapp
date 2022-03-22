package nl.tudelft.trustchain.FOC

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.frostwire.jlibtorrent.*
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
    private val saveDir: File,
    private val sessionManager: SessionManager,
    private val context: Context,
    private val activity: MainActivityFOC
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val maxTorrentThreads = 5
    private val torrentHandles = ArrayList<TorrentHandle>()
    private val torrentInfos = ArrayList<TorrentInfo>()
    private val failedTorrents = HashMap<String, Long>()
    var sessionActive = false

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */

    companion object {
        fun getInstance(
            saveDir: File,
            sessionManager: SessionManager,
            context: Context,
            activity: MainActivityFOC
        ): AppGossiper {
            if (!::appGossiperInstance.isInitialized) {
                appGossiperInstance = AppGossiper(saveDir, sessionManager, context, activity)
            }
            return appGossiperInstance
        }
    }

    fun start() {
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
        val demoCommunity = IPv8Android.getInstance().getOverlay<DemoCommunity>()
        while (scope.isActive) {
            var torrentListMessages = demoCommunity?.getTorrentMessages()
            if (torrentListMessages != null) {
                for (packet in torrentListMessages) {
                    val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
                    Log.i("personal", peer.mid + ": " + payload.message)
                    var torrentName = payload.message.substringAfter("&dn=")
                        .substringBefore('&')
                    var magnetLink = payload.message.substringAfter("FOC:")
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
        saveDir.listFiles()?.forEachIndexed { _, file ->
            if (file.name.endsWith(".torrent")) {
                val torrentInfo = TorrentInfo(file)
                if (torrentInfo.isValid) {
                    // 'Downloading' the torrent file also starts seeding it after download has
                    // already been completed
                    // We only seed torrents that have previously already been fully downloaded
                    if (isTorrentOkay(torrentInfo, saveDir)) {
                        if (!torrentInfos.any { it.infoHash() == torrentInfo.infoHash() }) {
                            torrentInfos.add(torrentInfo)
                        }
                    }
                }
            }
        }
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

    private fun downloadAndSeed(torrentInfo: TorrentInfo, demoCommunity: DemoCommunity) {
        if (torrentInfo.isValid) {

            sessionManager.download(torrentInfo, saveDir)
            val torrentHandle = sessionManager.find(torrentInfo.infoHash()) ?: return
            torrentHandle.setFlags(torrentHandle.flags().and_(TorrentFlags.SEED_MODE))
            torrentHandle.pause()
            torrentHandle.resume() // This is a fix/hack that forces SEED_MODE to be available, for
            // an unsolved issue: seeding local torrents often result in an endless "CHECKING_FILES"
            // state
            val magnet_link = "magnet:?xt=urn:btih:" + torrentInfo.infoHash() + "&dn=" + torrentInfo.name()
            demoCommunity.informAboutTorrent(magnet_link)
            torrentHandles.add(torrentHandle)
        }
    }

    fun isTorrentOkay(torrentInfo: TorrentInfo, saveDirectory: File): Boolean {
        val file = File(saveDirectory.path + "/" + torrentInfo.name())
        if (!(file.extension == "apk" || file.extension == "jar")) return false
        if (file.length() >= torrentInfo.totalSize()) return true
        return false
    }

    @Suppress("deprecation")
    fun getMagnetLink(magnetLink: String, torrentName: String) {
        // Handling of the case where the user is already downloading the
        // same or another torrent

        if (sessionActive) {
            return
        }

        if (!magnetLink.startsWith("magnet:")) {
            return
        } else {
            val startindexname = magnetLink.indexOf("&dn=")
            val stopindexname =
                if (magnetLink.contains("&tr=")) magnetLink.indexOf("&tr") else magnetLink.length

            val magnetnameraw = magnetLink.substring(startindexname + 4, stopindexname)
            Log.i("personal", magnetnameraw)
            val magnetname = magnetnameraw.replace('+', ' ', false)
            Log.i("personal", magnetname)
        }

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
            failedTorrents[torrentName] = System.currentTimeMillis()
            return
        }

        if (data != null) {
            val torrentInfo = Entry.bdecode(data).toString()
            Log.i("personal", torrentInfo)

            val ti = TorrentInfo.bdecode(data)
            sessionActive = true
            activity.signal = CountDownLatch(1)
            sessionManager.download(ti, saveDir)
            activity.signal.await(45, TimeUnit.SECONDS)
            if (activity.signal.count.toInt() == 1) {
                activity.runOnUiThread { printToast("Attempt to download timed out for $torrentName!") }
                activity.signal = CountDownLatch(0)
                failedTorrents[torrentName] = System.currentTimeMillis()
            }
            sessionActive = false
        } else {
            Log.i("personal", "Failed to retrieve the magnet")
            activity.runOnUiThread { printToast("Failed to retrieve magnet for $torrentName!") }
            failedTorrents[torrentName] = System.currentTimeMillis()
        }
    }

    fun printToast(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_LONG).show()
    }

}
