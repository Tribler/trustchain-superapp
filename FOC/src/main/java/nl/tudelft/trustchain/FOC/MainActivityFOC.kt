package nl.tudelft.trustchain.FOC

import android.Manifest
import android.content.Context
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
import dalvik.system.DexClassLoader
import kotlinx.android.synthetic.main.activity_main_foc.*
import kotlinx.android.synthetic.main.content_main_activity_foc.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.DemoCommunity
import nl.tudelft.trustchain.common.MyMessage
import java.io.*
import java.nio.channels.FileChannel
import java.util.*
import java.util.concurrent.CountDownLatch


class MainActivityFOC : AppCompatActivity() {

    private var arrayList = ArrayList<String>()//Creating an empty arraylist

    private lateinit var adapter : ArrayAdapter<String>

    var counter = 0

    private var uploadingTorrent = "greatBigTorrent"

   val mobileArray = arrayOf("Android","IPhone","WindowsMobile","Blackberry",
      "WebOS","Ubuntu","Windows7","Max OS X")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_foc)
        setSupportActionBar(toolbar)

        val listView = myListView as ListView
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE)
        adapter = ArrayAdapter<String>(
            this,
             android.R.layout.simple_list_item_1, arrayList
        )
        listView.setAdapter(adapter)

        listView.setOnItemClickListener { parent, _, position, _ ->
            var item = parent.getItemAtPosition(position);
            printToast(item.toString());
            enterTorrent.setText(item.toString().substringAfter(" - "))
        };


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
        informPeersButton.setOnClickListener { _ ->
            val ipv8 = IPv8Android.getInstance()
            val demoCommunity = ipv8.getOverlay<DemoCommunity>()!!
            val peers = demoCommunity.getPeers()

            Log.i("personal", "n:" + peers.size.toString())
            for (peer in peers) {
                Log.i("personal", peer.mid)
            }

            //demoCommunity.broadcastGreeting()
            //printToast("Greeted " + peers.size.toString() + " peers")

            //CreateTorrentTest.testFromFile();

            demoCommunity.informAboutTorrent(uploadingTorrent)



        }

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

    fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
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

    /**
     * Download a torrent through a magnet link
     */
    fun getMagnetLink() {
        val magnetLink: String?
        val inputText = enterTorrent.text.toString()
        if (inputText == "") {
            printToast("No magnet link given, using default")
            // magnetLink = "magnet:?xt=urn:btih:86d0502ead28e495c9e67665340f72aa72fe304e&dn=Frostwire.5.3.6.+%5BWindows%5D&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80&tr=udp%3A%2F%2Ftracker.istole.it%3A6969&tr=udp%3A%2F%2Fopen.demonii.com%3A1337";
            // magnetLink = "magnet:?xt=urn:btih:737d38ed01da1df727a3e0521a6f2c457cb812de&dn=HOME+-+a+film+by+Yann+Arthus-Bertrand+%282009%29+%5BEnglish%5D+%5BHD+MP4%5D&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.zer0day.to%3A1337&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969";
            // magnetLink = "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c";
            magnetLink = "magnet:?xt=urn:btih:209c8226b299b308beaf2b9cd3fb49212dbd13ec&dn=Tears+of+Steel&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F&xs=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2Ftears-of-steel.torrent"
            // magnetLink = "magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F&xs=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2Fbig-buck-bunny.torrent";
        } else magnetLink = inputText

        val s = SessionManager()

        val sp = SettingsPack()

        val params =
            SessionParams(sp)

        val signal = CountDownLatch(1)

        s.addListener(object : AlertListener {
            override fun types(): IntArray? {
                return null
            }

            override fun alert(alert: Alert<*>) {
                val type = alert.type()

                when (type) {
                    AlertType.ADD_TORRENT -> {
                        Log.i("personal", "Torrent added")
                        // System.out.println("Torrent added");
                        (alert as AddTorrentAlert).handle().resume()
                    }
                    AlertType.BLOCK_FINISHED -> {
                        val a = alert as BlockFinishedAlert
                        val p = (a.handle().status().progress() * 100).toInt()
                        progressBar.setProgress(p, true)
                        Log.i("personal", "Progress: " + p + " for torrent name: " + a.torrentName())
                        Log.i("personal", java.lang.Long.toString(s.stats().totalDownload()))
                    }
                    AlertType.TORRENT_FINISHED -> {
                        progressBar.setProgress(100, true)
                        Log.i("personal", "Torrent finished")
                        printToast("Torrent downloaded!!")
                        signal.countDown()

                        s.stop()
                    }
                    else -> {}
                }
            }
        })

        s.start(params)

        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val nodes = s.stats().dhtNodes()
                // wait for at least 10 nodes in the DHT.
                if (nodes >= 10) {
                    Log.i("personal", "DHT contains $nodes nodes")
                    // signal.countDown();
                    timer.cancel()
                }
            }
        }, 0, 1000)

        printToast("Starting download, please wait...")

        Log.i("personal", "Fetching the magnet uri, please wait...")
        val data = s.fetchMagnet(magnetLink, 30)

        if (data != null) {
            val torrentInfo = Entry.bdecode(data).toString()
            Log.i("personal", torrentInfo)
            torrentView.text = torrentInfo

            val ti = TorrentInfo.bdecode(data)
            s.download(ti, File("/storage/emulated/0"))
        } else {
            Log.i("personal", "Failed to retrieve the magnet")
            printToast("Something went wrong, check logs")
        }
    }

    /**
     *  Download a torrent through a .torrent file on your phone
     */
    @Suppress("deprecation")
    fun getTorrent(seed : Boolean) {

        val torrentName: String?
        val inputText = enterTorrent.text.toString()
        if (inputText == "") {
            printToast("No torrent name given, using default")
            torrentName = "sintel.torrent"
        } else torrentName = inputText
        val torrent =
            Environment.getExternalStorageDirectory().absolutePath + "/" + torrentName
        try {
            if (!readTorrentSuccesfully(torrent)) {
                //printToast("Something went wrong, check logs")
                return
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val s = SessionManager()

        val sp = SettingsPack()

        Log.i("personal", sp.seedingOutgoingConnections().toString());
        if (seed)
            sp.seedingOutgoingConnections(true)
        Log.i("personal", sp.seedingOutgoingConnections().toString());

        val params =
            SessionParams(sp)

        //val signal = CountDownLatch(1)

        s.addListener(object : AlertListener {
            override fun types(): IntArray? {
                return null
            }

            override fun alert(alert: Alert<*>) {
                val type = alert.type()

                when (type) {
                    AlertType.ADD_TORRENT -> {
                        Log.i("personal", "Torrent added")
                        // System.out.println("Torrent added");
                        (alert as AddTorrentAlert).handle().resume()
                    }
                    AlertType.BLOCK_FINISHED -> {
                        val a = alert as BlockFinishedAlert
                        val p = (a.handle().status().progress() * 100).toInt()
                        progressBar.setProgress(p, true)
                        Log.i("personal", "Progress: " + p + " for torrent name: " + a.torrentName())
                        Log.i("personal", java.lang.Long.toString(s.stats().totalDownload()))
                    }
                    AlertType.TORRENT_FINISHED -> {
                        progressBar.setProgress(100, true)
                        Log.i("personal", "Torrent finished")
                        printToast("Torrent downloaded!!")
                        //signal.countDown()

                        if (!seed)
                        s.stop()
                    }
                    else -> {
                        //Log.i("personal", "something")
                    }
                }
            }
        })

        s.start(params)

        //val data: String = "8419d694e1049229bae56b5418a277be75aa9f15"
        //s.dhtPutItem(Entry(data)).toString()

        //printToast("Starting download, please wait...")

        val torrentFile = File(torrent)
        val ti = TorrentInfo(torrentFile)

        Log.i("personal", "Storage of downloads: " + torrentFile.parentFile!!.toString())

        s.download(ti, torrentFile.parentFile)

        /*
        val thread: Thread = object : Thread() {
            override fun run() {
                while (true) {
                    try {
                        sleep(5000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    Log.i("personal", "is Running: "  + s.isRunning.toString());
                    Log.i("personal", "total upload: " + s.totalUpload().toString());
                }
            }
        }
        thread.start()

         */

        //System.`in`.read()
    }

    /**
     * Reads a .torrent file and displays information about it on the screen
     * Part of the getTorrent() function
     */
    @Throws(IOException::class)
    fun readTorrentSuccesfully(torrent: String?): Boolean {
        val torrentFile = File(torrent!!)

        if (!torrentFile.exists()) {
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

    /**
     * Dynamically load and execute code from a jar/apk file
     * The name of the class to be loaded, and the name of the
     * function to execute, have to be known beforehand
     */
    @Suppress("deprecation", "unchecked_cast")
    fun loadDynamicCode() {
        try {
            val jarName: String?
            val inputText = enterJar.text.toString()
            if (inputText == "") {
                printToast("No jar/apk given, using default")
                jarName = "Injected.jar"
            } else jarName = inputText
            val libPath =
                Environment.getExternalStorageDirectory().absolutePath + "/" + jarName
            val dexOutputDir = getDir("dex", Context.MODE_PRIVATE)
            val tmpDir = File(libPath)
            val exists = tmpDir.exists()
            val extStore =
                Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
            if (exists && extStore) Log.i("personal", "exists")
            else {
                printToast("Something went wrong, check logs")
                return
            }
            val classloader = DexClassLoader(
                libPath, dexOutputDir.absolutePath, null,
                this.javaClass.classLoader
            )
            val classToLoad =
                classloader.loadClass("com.example.injected.Injected") as Class<Any>
            // final Class<Object> classToLoad = (Class<Object>) classloader.loadClass("p000.Example");
            val myInstance = classToLoad.newInstance()
            val printStuff = classToLoad.getMethod("printStuff")
            printStuff.invoke(myInstance)
            printToast("Check your logs for interdimensional message!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createTorrent(){

        val fileName: String?
        val inputText = enterTorrent.text.toString()
        if (inputText == "") {
            printToast("No torrent name given, using default")
            fileName = "image.png"
        } else fileName = inputText

        val file =
            File(Environment.getExternalStorageDirectory().absolutePath + "/" + fileName)
        if (!file.exists()) {
            Log.i("personal", "doesnt exist")
        }
        //Utils.writeByteArrayToFile(f, new byte[]{0}, false);
        val fs = file_storage()
        val l1: add_files_listener = object : add_files_listener() {
            override fun pred(p: String): Boolean { //assertEquals(f.getAbsolutePath(), p);
                return true
            }
        }
        libtorrent.add_files_ex(fs, file.absolutePath, l1, create_flags_t())
        val ct = create_torrent(fs)
        val l2: set_piece_hashes_listener = object : set_piece_hashes_listener() {
            override fun progress(i: Int) { }
        }

        val ec = error_code()
        libtorrent.set_piece_hashes_ex(ct, file.parent, l2, ec)
        val torrent = ct.generate()
        val buffer = torrent.bencode()

        var torrentName = fileName.substringBeforeLast('.') + ".torrent"

        var os: OutputStream? = null
        try {
            os =
                FileOutputStream(File(
                    Environment.getExternalStorageDirectory().absolutePath + "/" + torrentName))
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

    fun retrieveListOfAvailableTorrents(){
        val ipv8 = IPv8Android.getInstance()
        val demoCommunity = ipv8.getOverlay<DemoCommunity>()!!
        var torrentListMessages = demoCommunity.getTorrentMessages()
        for (packet in torrentListMessages){
            val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
            Log.i("personal", peer.mid + ": " + payload.message)
            var magnetLink = payload.message.substringAfter("FOC:")
            var torrentName = payload.message.substringAfter("&dn=").
                substringBefore('&')
            var containsItem = false;
            for (i in 0..adapter.count-1){
                if (adapter.getItem(i).equals(torrentName)){
                    containsItem = true
                    break
                }
            }
            if (!containsItem) {
                adapter.add(torrentName + " - " + magnetLink);
                setListViewHeightBasedOnChildren(myListView)
            }

        }

        //arrayList.add("blabla" + counter++);
        //adapter.notifyDataSetChanged();
    }

    fun setListViewHeightBasedOnChildren(listView : ListView) {
        var listAdapter: ListAdapter = listView.getAdapter()

        var totalHeight = 0;
        var desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        for (i in 0..listAdapter.getCount()-1) {
            var listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        var params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}


