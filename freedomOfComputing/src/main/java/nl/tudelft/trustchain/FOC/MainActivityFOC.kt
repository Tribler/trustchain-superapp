package nl.tudelft.trustchain.FOC

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.DemoCommunity
import nl.tudelft.trustchain.common.MyMessage
import java.io.*
import java.nio.channels.FileChannel
import java.util.*

class MainActivityFOC : AppCompatActivity() {

    val s = SessionManager()
    var sessionActive = false

    private var torrentList = ArrayList<String>() // Creating an empty arraylist

    private lateinit var adapterLV: ArrayAdapter<String>

    private var uploadingTorrent = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_foc)
        setSupportActionBar(toolbar)

        initializeTorrentSession()

        // create a list view for any incoming torrents
        // that are seeded
        val listView = myListView as ListView
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE)
        adapterLV = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, torrentList)
        listView.setAdapter(adapterLV)

        // whenever an available torrent is seeded, clicking on it
        // inserts it into the input for torrent names/magnet links
        listView.setOnItemClickListener { parent, _, position, _ ->
            var item = parent.getItemAtPosition(position)
            enterTorrent.setText(item.toString().substringAfter(" - "))
        }

        printToast("STARTED")

        // option 1: download a torrent through a magnet link
        downloadMagnetButton.setOnClickListener { _ ->
            getMagnetLink()
        }

        // option 2: download a torrent through a .torrent file on your phone
        downloadTorrentButton.setOnClickListener { _ ->
            getTorrent(false)
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
    fun printToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
    }

    fun initializeTorrentSession() {
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
                        progressBar.setProgress(p, true)
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

        if (sessionActive) {
            s.stop()
            sessionActive = false
            downloadTorrentButton.setText("DOWNLOAD (TORRENT)")
            if (downloadMagnetButton.text.equals("STOP")) {
                downloadMagnetButton.setText("DOWNLOAD (MAGNET LINK)")
                return
            } else {
                torrentView.text = ""
                progressBar.setProgress(0, true)
            }
        }

        val magnetLink: String?
        val inputText = enterTorrent.text.toString()
        if (inputText == "") {
            printToast("No magnet link given, using default")
            // magnetLink = "magnet:?xt=urn:btih:86d0502ead28e495c9e67665340f72aa72fe304e&dn=Frostwire.5.3.6.+%5BWindows%5D&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80&tr=udp%3A%2F%2Ftracker.istole.it%3A6969&tr=udp%3A%2F%2Fopen.demonii.com%3A1337";
            // magnetLink = "magnet:?xt=urn:btih:737d38ed01da1df727a3e0521a6f2c457cb812de&dn=HOME+-+a+film+by+Yann+Arthus-Bertrand+%282009%29+%5BEnglish%5D+%5BHD+MP4%5D&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.zer0day.to%3A1337&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969";
            // magnetLink = "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c";
            magnetLink =
                "magnet:?xt=urn:btih:209c8226b299b308beaf2b9cd3fb49212dbd13ec&dn=Tears+of+Steel&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F&xs=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2Ftears-of-steel.torrent"
            // magnetLink = "magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F&xs=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2Fbig-buck-bunny.torrent";
        } else magnetLink = inputText

        if (!magnetLink.startsWith("magnet:")) {
            printToast("This is not a magnet link")
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

        printToast("Starting download, please wait...")

        Log.i("personal", "Fetching the magnet uri, please wait...")
        var data: ByteArray
        try {
            data = s.fetchMagnet(magnetLink, 30)
        } catch (e: Exception) {
            Log.i("personal", "Failed to retrieve the magnet")
            printToast("Something went wrong, check logs")
            return
        }

        if (data != null) {
            val torrentInfo = Entry.bdecode(data).toString()
            Log.i("personal", torrentInfo)
            torrentView.text = torrentInfo

            val ti = TorrentInfo.bdecode(data)
            sessionActive = true
            downloadMagnetButton.setText("STOP")
            // val savePath = applicationContext.getExternalFilesDir(null)!!.getAbsolutePath()
            // uncomment if you want to write to the actual phone storage (needs "write" permission)
            val savePath = Environment.getExternalStorageDirectory().absolutePath
            s.download(ti, File(savePath))
        } else {
            Log.i("personal", "Failed to retrieve the magnet")
            printToast("Something went wrong, check logs")
        }
    }

    /**
     *  Download a torrent through a .torrent file on your phone
     */
    @Suppress("deprecation")
    fun getTorrent(uploadHappening: Boolean) {

        // Handling of the case where the user is already downloading the
        // same or another torrent
        if (sessionActive) {
            s.stop()
            sessionActive = false
            downloadMagnetButton.setText("DOWNLOAD (MAGNET LINK)")
            if (downloadTorrentButton.text.equals("STOP")) {
                downloadTorrentButton.setText("DOWNLOAD (TORRENT)")
                return
            } else {
                torrentView.text = ""
                progressBar.setProgress(0, true)
            }
        }

        val torrentName: String?
        val inputText = enterTorrent.text.toString()
        if (inputText == "") {
            printToast("No torrent name given, using default")
            torrentName = "sintel.torrent"
        } else torrentName = inputText

        // uncomment if you want to read from the actual phone storage (needs "write" permission)
        var torrent = Environment.getExternalStorageDirectory().absolutePath + "/" + torrentName
        // if (uploadHappening) {
        // val torrent = Environment.getExternalStorageDirectory().absolutePath + "/" + torrentName
        // torrent =
        //    applicationContext.getExternalFilesDir(null)!!.getAbsolutePath() + "/" + torrentName
        // }
        try {
            if (!readTorrentSuccesfully(torrent)) {
                printToast("Something went wrong, check logs")
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
            printToast("Starting upload, please wait...")
        else printToast("Starting download, please wait...")

        val torrentFile = File(torrent)
        val ti = TorrentInfo(torrentFile)

        Log.i("personal", "Storage of downloads: " + torrentFile.parentFile!!.toString())

        sessionActive = true
        if (!uploadHappening)
            downloadTorrentButton.setText("STOP")
        // uncomment if you want to write to the actual phone storage (needs "write" permission)
        s.download(ti, torrentFile.parentFile)
        // val savePath = applicationContext.getExternalFilesDir(null)!!.getAbsolutePath()
        // s.download(ti, File(savePath))
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
            // uncomment if you want to read from the actual phone storage (needs "write" permission)
            intent.putExtra(
                "fileName",
                Environment.getExternalStorageDirectory().absolutePath + "/" + apkName
            )
            // intent.putExtra("fileName", apkName);
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*
    Creates a torrent from a file given as input
    The extension of the file must be included (for example, .png)
     */
    @Suppress("deprecation")
    fun createTorrent() {
        val fileName: String?
        val inputText = enterTorrent.text.toString()
        if (inputText == "") {
            printToast("No torrent name given, using default")
            fileName = "image.png"
        } else fileName = inputText

        val file =
            File(Environment.getExternalStorageDirectory().absolutePath + "/" + fileName)
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

        var torrentName = fileName.substringBeforeLast('.') + ".torrent"

        var os: OutputStream? = null
        try {
            // uncomment if you want to write to the actual phone storage (needs "write" permission)
            os =
                FileOutputStream(File(Environment.getExternalStorageDirectory().absolutePath + "/" + torrentName))

            // os = FileOutputStream(File(applicationContext.getExternalFilesDir(null)!!.getAbsolutePath() + "/" + torrentName))

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
        val magnet_link = "magnet:?xt=urn:btih:" + ti.infoHash() + "&dn=" + ti.name()
        uploadingTorrent = magnet_link
        Log.i("personal", magnet_link)

        enterTorrent.setText(torrentName)
        getTorrent(true)
    }

    /*
    Displays the list of all the torrents being seeded at the moment,
    based on the messages received from those peers that seed
     */
    fun retrieveListOfAvailableTorrents() {
        val ipv8 = IPv8Android.getInstance()
        val demoCommunity = ipv8.getOverlay<DemoCommunity>()!!
        var torrentListMessages = demoCommunity.getTorrentMessages()
        for (packet in torrentListMessages) {
            val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
            Log.i("personal", peer.mid + ": " + payload.message)
            var magnetLink = payload.message.substringAfter("FOC:")
            var torrentName = payload.message.substringAfter("&dn=")
                .substringBefore('&')
            var containsItem = false
            for (i in 0..adapterLV.count - 1) {
                if (adapterLV.getItem(i) != null && adapterLV.getItem(i)!!
                    .startsWith(torrentName)
                ) {
                    containsItem = true
                    break
                }
            }
            if (!containsItem) {
                adapterLV.add(torrentName + " - " + magnetLink)
                setListViewHeightBasedOnChildren(myListView)
            }
        }
    }

    /*
    Handles correct viewing of our list of torrents, since it is within a ScrollView
     */
    fun setListViewHeightBasedOnChildren(listView: ListView) {
        var listAdapter: ListAdapter = listView.getAdapter()

        var totalHeight = 0
        var desiredWidth =
            View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST)
        for (i in 0..listAdapter.getCount() - 1) {
            var listItem = listAdapter.getView(i, null, listView)
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
            totalHeight += listItem.getMeasuredHeight()
        }

        var params = listView.getLayoutParams()
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1))
        listView.setLayoutParams(params)
        listView.requestLayout()
    }
}
