package nl.tudelft.trustchain.FOC

import android.content.Context
import com.frostwire.jlibtorrent.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.Key
import nl.tudelft.trustchain.FOC.community.FOCMessage
import nl.tudelft.trustchain.FOC.util.MagnetUtils.Companion.constructMagnetLink
import org.awaitility.Awaitility.await
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit
import kotlin.Pair

/**
 * App Gossiping Tests
 */
class AppGossiperTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var mainActivity: MainActivityFOC
    private lateinit var focCommunityMock: FOCCommunityMock
    private lateinit var appGossiper: AppGossiper
    private lateinit var cacheDir: File

    private val firstPeerKey = mockk<Key>()
    private val firstPeerKeyHash = byteArrayOf(0, 1, 2)
    private val peer = Peer(firstPeerKey)
    private val someTorrentHash = Sha1Hash(
        ByteArray(20) {
            5.toByte()
        }
    )
    private val someTorrentName = "some-torrent"
    private val contextDir = "src/test/resources"
    private val filesToUpload = arrayOf(File("$contextDir/some.torrent"))

    private val someMagnetLink = constructMagnetLink(someTorrentHash, someTorrentName)
    private val someMagnetLinkByteArray = MagnetByteArray.sampleMagnetByteArray
    private val payload = FOCMessage(someMagnetLink)
    private val firstIncomingPacket = Pair(peer, payload)

    @OptIn(ExperimentalStdlibApi::class)
    @Before
    fun setUp() {
        sessionManager = mockk(relaxed = true)
        val stats = mockk<SessionStats>()
        every { stats.dhtNodes() } returns (15)
        every { sessionManager.stats() } returns (stats)

        every { firstPeerKey.keyToHash() } returns (firstPeerKeyHash)

        mainActivity = mockk(relaxed = true)
        val context = mockk<Context>()
        cacheDir = mockk<File>()
        every { cacheDir.listFiles() } returns (filesToUpload)
        every { cacheDir.path } returns (contextDir)
        every { context.cacheDir } returns (cacheDir)
        every { mainActivity.applicationContext } returns (context)

        focCommunityMock = FOCCommunityMock("some-id")
        focCommunityMock.addTorrentMessages(firstIncomingPacket)
    }

    @Test
    fun triesToDownloadGossipedTorrentFromMagnetLink() {
        every { sessionManager.fetchMagnet(someMagnetLink, 30) } returns (someMagnetLinkByteArray)
        appGossiper = AppGossiper(sessionManager, mainActivity, focCommunityMock, false)
        appGossiper.start()
        await().atMost(1, TimeUnit.MINUTES).until { appGossiper.signal.count.toInt() == 1 }
        val torrentInfoSlot = mutableListOf<TorrentInfo>()
        verify(exactly = 1) { sessionManager.fetchMagnet(someMagnetLink, 30) }
        verify(exactly = 2) { sessionManager.download(capture(torrentInfoSlot), cacheDir) }
        Assert.assertEquals(
            torrentInfoSlot.map { it.infoHash() }.sorted(),
            listOf(TorrentInfo.bdecode(someMagnetLinkByteArray).infoHash(), TorrentInfo(File(contextDir + "/some.torrent")).infoHash()).sorted()
        )
    }

    @Test
    fun usesEvaProtocolAsFallbackWhenTorrentingDoesntWork() {
        every { sessionManager.fetchMagnet(someMagnetLink, 30) } throws RuntimeException("No torrenting for you")
        appGossiper = AppGossiper(sessionManager, mainActivity, focCommunityMock, false)
        appGossiper.start()
        await().atMost(1, TimeUnit.MINUTES).until { focCommunityMock.appRequests.size == 1 }
        val appRequest = focCommunityMock.appRequests[0]
        Assert.assertEquals(someTorrentHash.toString(), appRequest.first)
        Assert.assertEquals(firstPeerKeyHash, appRequest.second.key.keyToHash())
    }

    @Test
    fun informsPeersAboutLocalFiles() {
        every { sessionManager.fetchMagnet(someMagnetLink, 30) } throws RuntimeException("No torrenting for you")
        val mockTorrentHandle = mockk<TorrentHandle>(relaxed = true)
        every { mockTorrentHandle.isValid } returns true
        every { sessionManager.find(any()) } returns mockTorrentHandle

        appGossiper = AppGossiper(sessionManager, mainActivity, focCommunityMock, false)
        appGossiper.start()
        await().atMost(1, TimeUnit.MINUTES).until { focCommunityMock.torrentsInformedAbout.size == 1 }
        val torrentInformedAbout = focCommunityMock.torrentsInformedAbout[0]
        val torrentHashInfo =
            constructMagnetLink(TorrentInfo(File(contextDir + "/some.torrent")).infoHash(), "some.apk")
        Assert.assertEquals(torrentHashInfo, torrentInformedAbout)
    }
}
