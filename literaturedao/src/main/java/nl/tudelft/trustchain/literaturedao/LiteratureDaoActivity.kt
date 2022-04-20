package nl.tudelft.trustchain.literaturedao
import LiteratureGossiper
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.SearchView
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.Vectors
import com.frostwire.jlibtorrent.swig.*
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import nl.tudelft.trustchain.literaturedao.utils.ExtensionUtils.Companion.torrentDotExtension
import nl.tudelft.trustchain.literaturedao.utils.MagnetUtils.Companion.displayNameAppender
import nl.tudelft.trustchain.literaturedao.utils.MagnetUtils.Companion.preHashString
import java.io.*
import java.lang.Exception
import java.util.*
import kotlin.math.roundToInt
import java.util.concurrent.locks.ReentrantLock

const val DEFAULT_LITERATURE = "1.pdf"

open class LiteratureDaoActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_literaturedao
    override val bottomNavigationMenu = R.menu.literature_navigation_menu
    val metaDataLock = ReentrantLock()
    val scope = CoroutineScope(Job() + Dispatchers.Main)

    private val scope = CoroutineScope(Dispatchers.IO)
    var torrentList = ArrayList<Button>()
    private var progressVisible = false
    private var debugVisible = false
    private var bufferSize = 1024 * 5
    private val s = SessionManager()
    private var torrentAmount = 0

    private var literatureGossiper: LiteratureGossiper? = null

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

        try {
            Log.e("litdao", "starting ...")

            copyDefaultLiterature()

            literatureGossiper =
                IPv8Android.getInstance().getOverlay<LiteratureCommunity>()?.let { LiteratureGossiper.getInstance(s, this, it) }
            literatureGossiper?.start()


        } catch (e: Exception) {
            printToast(e.toString())
        }


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
            //Log.e("litdao", loadMetaData().toString())
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
//        Log.e("litdao", "starting ...")
//
//        val searchView: SearchView = findViewById<SearchView>(R.id.searchViewLit)
//
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean {
//
//                return false
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                if (!newText.isNullOrEmpty())
//                    Log.d("litdao", localSearch(newText).toString())
//                return false
//            }
//        })
//        Log.d("litdao", localSearch("dpca").toString())

    }

    fun testImportPDF(){
        PDFBoxResourceLoader.init(getApplicationContext());
        var i = 1
        while (i < 4){
            importPDF(i.toString() + ".pdf")
            i += 1
        }
    }
    
        //KeyWordModelView(this.baseContext).calcKWs("1.pdf")
/*
        try{
            Log.e("litdao", "litDao read: " + read("1.pdf").content.toString())
        } catch (e: Exception){
            Log.e("litdao", "litDao exception: " + e.toString())
       }*/


    /**
     * Display a short message on the screen
     */
    private fun printToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_SHORT).show()
    }

    /**
     * Ensures that there will always be one apk runnable from within LitDao.
     */
    private fun copyDefaultLiterature() {
        try {
            val file = File(this.applicationContext.cacheDir.absolutePath + "/" + DEFAULT_LITERATURE)
            if (!file.exists()) {
                val outputStream = FileOutputStream(file)
                val ins = assets.open(DEFAULT_LITERATURE)
                outputStream.write(ins.readBytes())
                ins.close()
                outputStream.close()
                this.createTorrent(DEFAULT_LITERATURE)
            }
        } catch (e: Exception) {
            this.printToast(e.toString())
        }
    }

    /**
     * Creates a torrent from a file given as input
     * The extension of the file must be included (for example, .png)
     */
    private fun createTorrent(fileName: String): TorrentInfo? {
        val file = File(applicationContext.cacheDir.absolutePath + "/" + fileName.split("/").last())
        if (!file.exists()) {
            runOnUiThread { printToast("Something went wrong, check logs") }
            Log.i("litdao", "File doesn't exist!")
            return null
        }

        val fs = file_storage()
        val l1: add_files_listener = object : add_files_listener() {
            override fun pred(p: String): Boolean {
                return true
            }
        }
        libtorrent.add_files_ex(fs, file.absolutePath, l1, create_flags_t())
        val ct = create_torrent(fs)
        val l2: set_piece_hashes_listener = object : set_piece_hashes_listener() {
            override fun progress(i: Int) {}
        }

        val ec = error_code()
        libtorrent.set_piece_hashes_ex(ct, file.parent, l2, ec)
        val torrent = ct.generate()
        val buffer = torrent.bencode()

        val torrentName = fileName.substringBeforeLast('.') + torrentDotExtension

        var os: OutputStream? = null
        try {
            os = FileOutputStream(File(applicationContext.cacheDir, torrentName.split("/").last()))
            os.write(Vectors.byte_vector2bytes(buffer), 0, Vectors.byte_vector2bytes(buffer).size)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                os!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        val ti = TorrentInfo.bdecode(Vectors.byte_vector2bytes(buffer))
        val magnetLink = preHashString + ti.infoHash() + displayNameAppender + ti.name()
        Log.i("litdao", magnetLink)
        runOnUiThread { printToast(fileName) }
        return ti
    }

}


fun localSearch(inp: String): MutableList<Pair<String, Double>>{
    var handler = QueryHandler()
    return handler.scoreList(inp, loadAll())
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
    }
}
