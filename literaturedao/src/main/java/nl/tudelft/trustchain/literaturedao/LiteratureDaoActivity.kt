package nl.tudelft.trustchain.literaturedao
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.SearchView
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.common.DemoCommunity
import nl.tudelft.trustchain.literaturedao.controllers.KeywordExtractor
import nl.tudelft.trustchain.literaturedao.controllers.PdfController
import nl.tudelft.trustchain.literaturedao.controllers.QueryHandler
import nl.tudelft.trustchain.literaturedao.ipv8.LiteratureCommunity
import nl.tudelft.trustchain.literaturedao.ui.KeyWordModelView
import java.io.*
import java.util.*
import kotlin.math.roundToInt
import java.util.concurrent.locks.ReentrantLock


open class LiteratureDaoActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_literaturedao
    override val bottomNavigationMenu = R.menu.literature_navigation_menu
    val metaDataLock = ReentrantLock()
    val scope = CoroutineScope(Job() + Dispatchers.Main)

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

        fun listAssetFiles(path: String): List<String> {
            val assetManager = assets
            val files = assetManager.list(path)
            if (files != null) {
                return files.toList()
            }
            return listOf<String>()
        }
    }

    fun loadMetaData(): KeyWordModelView.Data{
        var fileInputStream: FileInputStream? = null
        try{
            fileInputStream = this.openFileInput("metaData")
        } catch (e: FileNotFoundException){
            this.openFileOutput("metaData", Context.MODE_PRIVATE).use { output ->
                output.write(Json.encodeToString(KeyWordModelView.Data(mutableListOf<Pair<String, MutableList<Pair<String, Double>>>>())).toByteArray())
            }
            fileInputStream = this.openFileInput("metaData")
        }
        var inputStreamReader: InputStreamReader = InputStreamReader(fileInputStream)
        val bufferedReader: BufferedReader = BufferedReader(inputStreamReader)
        val stringBuilder: StringBuilder = StringBuilder()
        var text: String? = null
        while ({ text = bufferedReader.readLine(); text }() != null) {
            stringBuilder.append(text)
            }
        return Json.decodeFromString<KeyWordModelView.Data>(stringBuilder.toString())
    }

    fun localSearch(inp: String): MutableList<Pair<String, Double>>{
        var handler = QueryHandler()
        return handler.scoreList(inp, loadMetaData().content)
    }

    fun writeMetaData(newData: KeyWordModelView.Data){
        metaDataLock.lock()
        this.openFileOutput("metaData", Context.MODE_PRIVATE).use { output ->
            output.write(Json.encodeToString(newData).toByteArray())
        }
        metaDataLock.unlock()
    }

    override fun onStart() {
        super.onStart()
        Log.e("litdao", "starting ...")

        try{
            //testImportPDF()
            Log.e("litdao", loadMetaData().toString())
        } catch (e: Exception){
            Log.e("litdao", "litDao exception: " + e.toString())
        }

        val searchView: SearchView = findViewById<SearchView>(R.id.searchViewLit)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {

                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty())
                    Log.d("litdao", localSearch(newText).toString())
                return false
            }
        })
        //Log.d("litdao", localSearch("dpca").toString())
    }

    fun testImportPDF(){
        PDFBoxResourceLoader.init(getApplicationContext());
        var i = 1
        while (i < 4){
            importPDF(i.toString() + ".pdf")
            i += 1
        }
    }

    @Serializable
    data class Data(val content: MutableList<Pair<String, MutableList<Pair<String, Double>>>>)

    fun operations(path: String, baseContext: Context){
        PDFBoxResourceLoader.init(baseContext)
        val csv: InputStream = getAssets().open("stemmed_freqs.csv")
        val pdf: InputStream = getAssets().open(path)
        val strippedString = PdfController().stripText(pdf)
        val kws = KeywordExtractor().extract(strippedString, csv)
        Log.e("litdao", "newWrite: " + kws.toString())
        metaDataLock.lock()
        var metadata = loadMetaData()
        metadata.content.add(Pair(path, kws))
        writeMetaData(metadata)
        metaDataLock.unlock()
    }

    fun importPDF(path: String){
        val context = this.baseContext
        operations(path, context)
        //operations(path, context)
    }
}

