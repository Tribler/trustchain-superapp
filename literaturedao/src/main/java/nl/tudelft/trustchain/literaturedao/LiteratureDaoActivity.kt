package nl.tudelft.trustchain.literaturedao

import LiteratureGossiper
import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.Vectors
import com.frostwire.jlibtorrent.swig.*
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.common.DemoCommunity
import nl.tudelft.trustchain.literaturedao.controllers.QueryHandler
import nl.tudelft.trustchain.literaturedao.data_types.Literature
import nl.tudelft.trustchain.literaturedao.data_types.LocalData
import nl.tudelft.trustchain.literaturedao.ipv8.LiteratureCommunity
import nl.tudelft.trustchain.literaturedao.ipv8.SearchResult
import nl.tudelft.trustchain.literaturedao.ipv8.SearchResultList
import nl.tudelft.trustchain.literaturedao.utils.ExtensionUtils.Companion.torrentDotExtension
import nl.tudelft.trustchain.literaturedao.utils.MagnetUtils.Companion.displayNameAppender
import nl.tudelft.trustchain.literaturedao.utils.MagnetUtils.Companion.preHashString
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt
import nl.tudelft.trustchain.literaturedao.utils.CacheUtil


const val DEFAULT_LITERATURE = "2.pdf"

open class LiteratureDaoActivity : BaseActivity() {

    // Setting Menu And Default routing
    override val navigationGraph = R.navigation.nav_literaturedao
    override val bottomNavigationMenu = R.menu.literature_navigation_menu
    private val myLiteratureFragment = MyLiteratureFragment();

    private val scope = CoroutineScope(Dispatchers.IO)
    var torrentList = ArrayList<Button>()
    private var progressVisible = false
    private var debugVisible = false
    private var bufferSize = 1024 * 5
    private val s = SessionManager()
    private var torrentAmount = 0

    private var literatureGossiper: LiteratureGossiper? = null

    var freqMap = emptyMap<String, Long>()
    var freqMapInitialized = false

    var remoteSearchList: MutableList<String> = mutableListOf()
    lateinit var remoteSearchListAdapter : ArrayAdapter<*>

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        /*
        setContentView(R.layout.activity_main);


        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container,myLiteratureFragment)
        transaction.commit();
*/


        val literatureCommunity = IPv8Android.getInstance().getOverlay<LiteratureCommunity>()!!
        printPeersInfo(literatureCommunity)
        val myName = literatureCommunity.myPeer.mid
        Log.i("litdao","I am $myName and Im broadcasting: hello")
        literatureCommunity.broadcastDebugMessage("hello")
        val parent = this

        /*scope.launch {
            instantiateAvgFreqMap(parent)
        }
        */

        val demoCommunity = IPv8Android.getInstance().getOverlay<DemoCommunity>()!!
        val demoCommunityName = demoCommunity.myPeer.mid
        Log.i("personal","I am $demoCommunityName and Im broadcasting a message")


        demoCommunity.broadcastGreeting()

        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide();

        try {
            Log.e("litdao", "starting ...")

            copyDefaultLiterature();

            // TODO fetch all local literatures.

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

        /*
        // TODO: UI CONNECTION FOR REMOTE SEARCH
        setContentView(R.layout.fragment_library_search)
        val remoteSearch = findViewById<SearchView>(R.id.remote_search_bar)
        remoteSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.i("litdao", "perform remote search with: "+query)
                if(!query.isNullOrBlank()){
                    remoteSeach(query)
                    return true
                }
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                Log.i("litdao", "remote search text changed to: "+query)
                return true
            }
        })

        remoteSearchListAdapter = ArrayAdapter(this, R.layout.fragment_library_search_row, remoteSearchList)
        findViewById<ListView>(R.id.remote_search_results).adapter = remoteSearchListAdapter
        */


        //debug space
        try{
            Log.e("litdao", "load localData: " + CacheUtil(baseContext).loadLocalData().toString())
            Log.e("litdao", "local search results: " + localSearch("Please de wit give me the scores").toString())
        } catch (e: Exception){
            Log.e("litdao", e.toString())
        }

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

    fun localSearch(inp: String): MutableList<Pair<String, Double>>{
        return CacheUtil(this.baseContext).localSearch(inp)
    }

    fun remoteSeach(query: String) {
        // send to peers
        IPv8Android.getInstance().getOverlay<LiteratureCommunity>()!!.broadcastSearchQuery(query)

//        // DEBUG
//        updateSearchResults(SearchResultList(listOf(SearchResult("f1", 1.0, "m1"), SearchResult("f2", 2.0, "m2"))))
    }

    fun updateSearchResults(results: SearchResultList){
        // access UI and append results to some view
        setContentView(R.layout.fragment_library_search)
        val list = findViewById<ListView>(R.id.remote_search_results)
        for (r : SearchResult in results.results){
            if(!remoteSearchList.contains(r.fileName)){
                remoteSearchList.add(r.fileName)
            }
        }
        remoteSearchListAdapter.notifyDataSetChanged()
    }
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
                val torrent = this.createTorrent(DEFAULT_LITERATURE)
                if (torrent != null) {
                    literatureGossiper?.addTorrentInfo(torrent)
                }
            }
        } catch (e: Exception) {
            this.printToast(e.toString())
        }
    }

    /**
     * Creates a torrent from a file in the cache directory
     * The extension of the file must be included (for example, .png)
     */
    fun createTorrent(fileName: String): TorrentInfo? {
        val file = File(applicationContext.cacheDir.absolutePath + "/" + fileName.split("/").last())
        if (!file.exists()) {
            runOnUiThread { printToast("Something went wrong, check logs") }
            Log.i("litdao", "File doesn't exist!")
            return null;
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
        runOnUiThread { printToast(fileName + " is ready for gossiping.") }
        return ti
    }



    @RequiresApi(Build.VERSION_CODES.M)
    fun filePicker(view: View) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, 100)
    }

    /**
     * copy file from source to destination
     *
     * @param src source
     * @param dst destination
     * @throws java.io.IOException in case of any problems
     */
    @Throws(IOException::class)
    fun copyFile(src: File?, dst: File?) {
        try {
            val inChannel = FileInputStream(src).channel
            val outChannel = FileOutputStream(dst).channel

            try {
                inChannel.transferTo(0, inChannel.size(), outChannel)
            } finally {
                inChannel.close()
                outChannel.close()
            }
        } catch (e: FileNotFoundException) {
            Log.e("litdao", e.toString())
        }
    }

    /**
     * Check storage permissions
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkStoragePermissions() {
        if ((ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_STORAGE_REQUEST_CODE
            )
        }
    }

    /**
     * Process permission result
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("litdao", "STORAGE PERMISSION GRANTED")
                printToast(resources.getString(R.string.litdao_permission_storage_granted))
            } else {
                Log.d("litdao", "STORAGE PERMISSION NOT GRANTED")
                printToast(resources.getString(R.string.litdao_permission_denied))
                finish()
            }
        }
    }

    companion object {
        const val PERMISSION_STORAGE_REQUEST_CODE = 2
    }
}
