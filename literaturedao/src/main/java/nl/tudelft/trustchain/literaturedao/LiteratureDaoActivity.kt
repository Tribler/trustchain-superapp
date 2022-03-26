package nl.tudelft.trustchain.literaturedao
import android.os.Bundle
import nl.tudelft.trustchain.common.BaseActivity
import com.frostwire.jlibtorrent.TorrentInfo
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.DemoCommunity
import nl.tudelft.trustchain.literaturedao.controllers.KeywordExtractor
import nl.tudelft.trustchain.literaturedao.controllers.PdfController
import nl.tudelft.trustchain.literaturedao.ipv8.LiteratureCommunity
import java.io.File
import java.io.FileOutputStream
import nl.tudelft.trustchain.literaturedao.controllers.QueryHandler
import java.io.InputStream
import java.util.*
import kotlin.math.roundToInt

open class LiteratureDaoActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_literaturedao
    override val bottomNavigationMenu = R.menu.literature_navigation_menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val literatureCommunity = IPv8Android.getInstance().getOverlay<LiteratureCommunity>()!!
        printPeersInfo(literatureCommunity)
        val myName = literatureCommunity.myPeer.mid
        Log.i("litdao","I am $myName and Im broadcasting: hello")
        literatureCommunity.broadcastDebugMessage("hello")

        val demoCommunity = IPv8Android.getInstance().getOverlay<DemoCommunity>()!!
        val demoCommunityName = demoCommunity.myPeer.mid
        Log.i("personal","I am $demoCommunityName and Im broadcasting a message")
        demoCommunity.broadcastGreeting()

        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide();


        //test seeding
//        val folderToShare = "assets"
////        val tor = SharedTorrent.create(albumFile, list, 65535, listOf(), "TrustChain-Superapp")
//        var torrentFile = "$folderToShare.torrent"
//        if (!File(torrentFile).isFile) {
//            torrentFile = "$folderToShare.torrent.added"
//        }
////        tor.save(FileOutputStream(torrentFile))
//        val torrentInfo = TorrentInfo(File(torrentFile))
//        val magnet = torrentInfo.makeMagnetUri()
//        val torrentInfoName = torrentInfo.name()

    }

    private fun printPeersInfo(overlay: Overlay) {
        val peers = overlay.getPeers()
        Log.i("litdao",overlay::class.simpleName + ": ${peers.size} peers")
        for (peer in peers) {
            val avgPing = peer.getAveragePing()
            val lastRequest = peer.lastRequest
            val lastResponse = peer.lastResponse

            val lastRequestStr = if (lastRequest != null)
                "" + ((Date().time - lastRequest.time) / 1000.0).roundToInt() + " s" else "?"

            val lastResponseStr = if (lastResponse != null)
                "" + ((Date().time - lastResponse.time) / 1000.0).roundToInt() + " s" else "?"

            val avgPingStr = if (!avgPing.isNaN()) "" + (avgPing * 1000).roundToInt() + " ms" else "? ms"
            Log.i("litdao", "${peer.mid} ${peer.address} (S: ${lastRequestStr}, R: ${lastResponseStr}, ${avgPingStr})")
        }
    }

    var tempStorage: MutableList<Pair<String, MutableList<Pair<String, Double>>>> = mutableListOf<Pair<String, MutableList<Pair<String, Double>>>>()

    override fun onStart() {
        super.onStart()
        Log.e("litdao", "starting ...")
        PDFBoxResourceLoader.init(getApplicationContext());
        var i = 1
        while (i < 2){
            val path = i.toString() + ".pdf"
            val stream: InputStream = getAssets().open(path)
            val kws = getKWs(stream)
            save(path, kws)
            Log.e("litdao", "litdao: " + kws.toString())
            i += 1
        }
        // query test
        var handler = QueryHandler()
        Log.d("litdao", handler.scoreList("Clustering all the algorighems man", loadAll()).toString())
        Log.d("litdao", handler.scoreList("The pythagorean algorithms machine learning", loadAll()).toString())

    }

    fun getKWs(pdfIS: java.io.InputStream): MutableList<Pair<String, Double>>{
        var pdfController = PdfController()
        val keywordExtractorInput = pdfController.stripText(pdfIS)
        val csv: InputStream = getAssets().open("stemmed_freqs.csv")
        val result = KeywordExtractor()
            .extract(keywordExtractorInput, csv)
        return result
    }


    fun save(path: String, KWList: MutableList<Pair<String, Double>>){
        tempStorage.add(Pair(path, KWList))
    }

    fun loadAll(): MutableList<Pair<String, MutableList<Pair<String, Double>>>>{
        return tempStorage
    }
}
