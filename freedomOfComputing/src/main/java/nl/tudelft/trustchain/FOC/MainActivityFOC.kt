package nl.tudelft.trustchain.FOC

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert
import com.frostwire.jlibtorrent.swig.*
import kotlinx.android.synthetic.main.activity_main_foc.*
import kotlinx.android.synthetic.main.content_main_activity_foc.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.DemoCommunity
import nl.tudelft.trustchain.common.MyMessage
import java.io.*
import java.nio.channels.FileChannel
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivityFOC : AppCompatActivity() {


    private val scope = CoroutineScope(Dispatchers.IO)
    val s = SessionManager()
    var sessionActive = false

    private var torrentList = ArrayList<String>() // Creating an empty arraylist

    var signal = CountDownLatch(0)

    private lateinit var adapterLV: ArrayAdapter<String>
    private lateinit var appGossiper: AppGossiper

    private var uploadingTorrent = ""

    @Suppress("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appGossiper = AppGossiper.getInstance(s, applicationContext, this)
        appGossiper.start()
        setContentView(R.layout.activity_main_foc)
        setSupportActionBar(toolbar)

        initializeTorrentSession()

        // create a list view for any incoming torrents
        // that are seeded
        val listView = myListView as ListView
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE
        adapterLV = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, torrentList)
        listView.adapter = adapterLV

        // whenever an available torrent is seeded, clicking on it
        // inserts it into the input for torrent names/magnet links
        listView.setOnItemClickListener { parent, _, position, _ ->
            val item = parent.getItemAtPosition(position)
            enterTorrent.setText(item.toString().substringAfter(" - "))
        }

        printToast("STARTED")

        // option 1: download a torrent through a magnet link
        downloadMagnetButton.setOnClickListener { _ ->
            scope.launch {
                getMagnetLink()
            }
        }

        // option 2: download a torrent through a .torrent file on your phone
        downloadTorrentButton.setOnClickListener { _ ->
            scope.launch {
                getTorrent(false)
            }
        }

        // option 3: Send a message to every other peer using the superapp
        informPeersButton.setOnClickListener { _ -> informPeersAboutSeeding() }

        // option 4: dynamically load and execute code from a jar/apk file
        executeCodeButton.setOnClickListener { _ ->
            loadDynamicCode()
        }

        uploadTorrentButton.setOnClickListener { _ ->
            createTorrent()
        }

        retrieveListButton.setOnClickListener { _ ->
            retrieveListOfAvailableTorrents()
        }

        // upon launching our activity, we ask for the "Storage" permission
        requestStoragePermission()
    }

    val MY_PERMISSIONS_REQUEST = 0

    // change if you want to write to the actual phone storage (needs "write" permission)
    fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) // READ_EXTERNAL_STORAGE
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), // READ_EXTERNAL_STORAGE
                MY_PERMISSIONS_REQUEST
            )
        }
    }

    /**
     * Display a short message on the screen
     */
    private fun printToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
    }

    private fun initializeTorrentSession() {
        s.addListener(object : AlertListener {
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            progressBar.setProgress(p, true)
                        }
                        Log.i(
                            "personal",
                            "Progress: " + p + " for torrent name: " + a.torrentName()
                        )
                        Log.i("personal", java.lang.Long.toString(s.stats().totalDownload()))
                    }
                    AlertType.TORRENT_FINISHED -> {
                        progressBar.setProgress(100, true)
                        downloadTorrentButton.setText("DOWNLOAD (TORRENT)")
                        downloadMagnetButton.setText("DOWNLOAD (MAGNET LINK)")
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
     * Download a torrent through a magnet link
     */
    @Suppress("deprecation")
    fun getMagnetLink() {
        // Handling of the case where the user is already downloading the
        // same or another torrent

        if (appGossiper.sessionActive && !sessionActive) {
            runOnUiThread { printToast("The torrent session is busy fetching files, please try again later") }
            return
        }

        if (sessionActive) {
            s.stop()
            sessionActive = false
            appGossiper.sessionActive = false
            downloadTorrentButton.text = "DOWNLOAD (TORRENT)"
            if (downloadMagnetButton.text.equals("STOP")) {
                downloadMagnetButton.text = "DOWNLOAD (MAGNET LINK)"
                return
            } else {
                torrentView.text = ""
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress(0, true)
                }
            }
        }

        val magnetLink = enterTorrent.text.toString()

        if (magnetLink == "") return

        if (!magnetLink.startsWith("magnet:")) {
            runOnUiThread { printToast("This is not a magnet link") }
            return
        } else {
            val startindexname = magnetLink.indexOf("&dn=")
            val stopindexname =
                if (magnetLink.contains("&tr=")) magnetLink.indexOf("&tr") else magnetLink.length

            val magnetnameraw = magnetLink.substring(startindexname + 4, stopindexname)
            Log.i("personal", magnetnameraw)
            val magnetname = magnetnameraw.replace('+', ' ', false)
            Log.i("personal", magnetname)
            enterJar.setText(magnetname)
        }

        val sp = SettingsPack()
        sp.seedingOutgoingConnections(true)
        val params =
            SessionParams(sp)
        s.start(params)

        val timer = Timer()
        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    val nodes = s.stats().dhtNodes()
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

        runOnUiThread { printToast("Starting download, please wait...") }

        Log.i("personal", "Fetching the magnet uri, please wait...")
        val data: ByteArray
        try {
            data = s.fetchMagnet(magnetLink, 30)
        } catch (e: Exception) {
            Log.i("personal", "Failed to retrieve the magnet")
            runOnUiThread { printToast("Something went wrong, check logs") }
            runOnUiThread { printToast(e.toString()) }
            return
        }

        if (data != null) {
            val torrentInfo = Entry.bdecode(data).toString()
            runOnUiThread { Log.i("personal", torrentInfo) }
            torrentView.text = torrentInfo

            val ti = TorrentInfo.bdecode(data)
            sessionActive = true
            appGossiper.sessionActive = true
            downloadMagnetButton.setText("STOP")
            val savePath = Environment.getExternalStorageDirectory().absolutePath
            s.download(ti, File(savePath))
        } else {
            Log.i("personal", "Failed to retrieve the magnet")
            runOnUiThread { printToast("Something went wrong, check logs") }
        }
        val ti = TorrentInfo.bdecode(data)
        sessionActive = true
        appGossiper.sessionActive = true
        downloadMagnetButton.text = "STOP"
        s.download(ti, applicationContext.cacheDir)
    }

    /**
     *  Download a torrent through a .torrent file on your phone
     */
    @Suppress("deprecation")
    fun getTorrent(uploadHappening: Boolean) {


        if (appGossiper.sessionActive && !sessionActive) {
            runOnUiThread { printToast("The torrent session is busy fetching files, please try again later") }
            return
        }

        // Handling of the case where the user is already downloading the
        // same or another torrent
        if (sessionActive) {
            s.stop()
            sessionActive = false
            appGossiper.sessionActive = false
            downloadMagnetButton.text = "DOWNLOAD (MAGNET LINK)"
            if (downloadTorrentButton.text.equals("STOP")) {
                downloadTorrentButton.text = "DOWNLOAD (TORRENT)"
                return
            } else {
                torrentView.text = ""
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress(0, true)
                }
            }
        }

        val torrentName: String?
        val inputText = enterTorrent.text.toString()
        if (inputText == "") {
            runOnUiThread { printToast("No torrent name given, using default") }
            torrentName = "sintel.torrent"
        } else torrentName = inputText

        val torrent = "${applicationContext.cacheDir}/${torrentName.split("/").last()}"

        try {
            if (!readTorrentSuccesfully(torrent)) {
                runOnUiThread { printToast("Something went wrong, check logs") }
                return
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val sp = SettingsPack()
        sp.seedingOutgoingConnections(true)
        val params =
            SessionParams(sp)
        s.start(params)

        if (uploadHappening)
            runOnUiThread { printToast("Starting upload, please wait...") }
        else runOnUiThread { printToast("Starting download, please wait...") }

        val torrentFile = File(torrent)
        val ti = TorrentInfo(torrentFile)

        Log.i("personal", "Storage of downloads: " + torrentFile.parentFile!!.toString())

        sessionActive = true
        appGossiper.sessionActive = true
        if (!uploadHappening)
            downloadTorrentButton.text = "STOP"
        s.download(ti, torrentFile.parentFile)
    }

    /**
     * Reads a .torrent file and displays information about it on the screen
     * Part of the getTorrent() function
     */
    @Throws(IOException::class)
    fun readTorrentSuccesfully(torrent: String?): Boolean {
        val torrentFile = File(torrent!!)

        if (!torrentFile.exists()) {
            Log.i("personal", "File doesn't exist!")
            return false
        }

        val ti = TorrentInfo(torrentFile)

        val fc = RandomAccessFile(torrent, "r").channel
        val buffer =
            fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
        val ti2 = TorrentInfo(buffer)
        val toPrint = ti.toEntry().toString() + ti2.toEntry().toString()
        Log.i("personal", ti.toEntry().toString())
        Log.i("personal", ti2.toEntry().toString())
        torrentView.text = toPrint
        return true
    }

    /* Let others peers know of the torrent you are seeding,
    by sending the magnet link
     */
    fun informPeersAboutSeeding() {
        val ipv8 = IPv8Android.getInstance()
        val demoCommunity = ipv8.getOverlay<DemoCommunity>()!!
        val peers = demoCommunity.getPeers()

        Log.i("personal", "n:" + peers.size.toString())
        for (peer in peers) {
            Log.i("personal", peer.mid)
        }

        demoCommunity.informAboutTorrent(uploadingTorrent)
    }

    /**
     * Dynamically load and execute code from a jar/apk file
     * The name of the class to be loaded, and the name of the
     * function to execute, have to be known beforehand
     */
    @Suppress("deprecation")
    fun loadDynamicCode() {
        val apkName: String?
        val inputText = enterJar.text.toString()
        if (inputText == "") {
            printToast("No apk/jar name given, using default")
            apkName = "demoapp.apk"
        } else apkName = inputText
        try {
            val intent = Intent(this, ExecutionActivity::class.java)
            intent.putExtra(
                "fileName",
                "${applicationContext.cacheDir}/${apkName.split("/").last()}"
            )
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Creates a torrent from a file given as input
     * The extension of the file must be included (for example, .png)
     */
    @Suppress("deprecation")
    fun createTorrent() {
        val fileName: String?
        val inputText = enterTorrent.text.toString()
        if (inputText == "") {
            printToast("No torrent name given, using default")
            fileName = "image.png"
        } else fileName = inputText

        val file = File(applicationContext.cacheDir, fileName.split("/").last())
        if (!file.exists()) {
            printToast("Something went wrong, check logs")
            Log.i("personal", "File doesn't exist!")
            return
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

        val torrentName = fileName.substringBeforeLast('.') + ".torrent"

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
        val magnetLink = "magnet:?xt=urn:btih:" + ti.infoHash() + "&dn=" + ti.name()
        uploadingTorrent = magnetLink
        Log.i("personal", magnetLink)

        enterTorrent.setText(torrentName)
        getTorrent(true)
    }

    /**
     * Displays the list of all the torrents being seeded at the moment,
     * based on the messages received from those peers that seed
     */
    fun retrieveListOfAvailableTorrents() {
        val ipv8 = IPv8Android.getInstance()
        val demoCommunity = ipv8.getOverlay<DemoCommunity>()!!
        val torrentListMessages = demoCommunity.getTorrentMessages()
        for (packet in torrentListMessages) {
            val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
            Log.i("personal", peer.mid + ": " + payload.message)
            val magnetLink = payload.message.substringAfter("FOC:")
            val torrentName = payload.message.substringAfter("&dn=")
                .substringBefore('&')
            var containsItem = false
            for (i in 0 until adapterLV.count) {
                if (adapterLV.getItem(i) != null && adapterLV.getItem(i)!!
                        .startsWith(torrentName)
                ) {
                    containsItem = true
                    break
                }
            }
            if (!containsItem) {
                adapterLV.add("$torrentName - $magnetLink")
                setListViewHeightBasedOnChildren(myListView)
            }
        }
    }

    /**
     * Handles correct viewing of our list of torrents, since it is within a ScrollView
     */
    fun setListViewHeightBasedOnChildren(listView: ListView) {
        val listAdapter: ListAdapter = listView.adapter

        var totalHeight = 0
        val desiredWidth =
            View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.AT_MOST)
        for (i in 0 until listAdapter.count) {
            val listItem = listAdapter.getView(i, null, listView)
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
            totalHeight += listItem.measuredHeight
        }

        val params = listView.layoutParams
        params.height = totalHeight + (listView.dividerHeight * (listAdapter.count - 1))
        listView.layoutParams = params
        listView.requestLayout()
    }
}
