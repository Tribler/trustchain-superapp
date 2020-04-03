package nl.tudelft.trustchain.FOC

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert
import kotlinx.android.synthetic.main.activity_main_foc.*
import kotlinx.android.synthetic.main.content_main_activity_foc.*
import nl.tudelft.ipv8.Overlay
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.math.roundToInt


class MainActivityFOC : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding = BlankFragmentBinding.inflate(layoutInflater)
        //val view = binding.root
        //setContentView(view)
        setContentView(R.layout.activity_main_foc)
        setSupportActionBar(toolbar)

        printToast("STARTED")

        //fab.setOnClickListener { view ->
        //    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //        .setAction("Action", null).show()
        //}

        greetPeersButton.setOnClickListener { view ->
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            //    .setAction("Action", null).show()
            //Toast.makeText(nl.tudelft.trustchain.common., "No magnet link provided, using default one...", Toast.LENGTH_LONG).show()
            getMagnet()


        }

        downloadTorrent.setOnClickListener { view ->
            Toast.makeText(applicationContext, "No magnet again", Toast.LENGTH_LONG).show()
        }



        //binding.downloadMagnet.setOnClickListener { view ->
        //    MainFunctionsJava.getMagnet(binding)
        //}

        MainFunctionsJava.requestPermission(this);

    }

    fun printPeersInfo(overlay: Overlay) {
        val peers = overlay.getPeers()
        //Log.i("personal", overlay::class.simpleName + ": ${peers.size} peers")
        for (peer in peers) {
            val avgPing = peer.getAveragePing()
            val lastRequest = peer.lastRequest
            val lastResponse = peer.lastResponse

            val lastRequestStr = if (lastRequest != null)
                "" + ((Date().time - lastRequest.time) / 1000.0).roundToInt() + " s" else "?"

            val lastResponseStr = if (lastResponse != null)
                "" + ((Date().time - lastResponse.time) / 1000.0).roundToInt() + " s" else "?"

            val avgPingStr = if (!avgPing.isNaN()) "" + (avgPing * 1000).roundToInt() + " ms" else "? ms"
            Log.i("personal","${peer.mid} (S: ${lastRequestStr}, R: ${lastResponseStr}, ${avgPingStr})")
        }
    }

    fun printToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
    }

    fun getMagnet(){
        var magnetLink: String? = null
        val inputText = enterTorrent.text.toString()
        if (inputText == "") { //String uri = "magnet:?xt=urn:btih:86d0502ead28e495c9e67665340f72aa72fe304e&dn=Frostwire.5.3.6.+%5BWindows%5D&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80&tr=udp%3A%2F%2Ftracker.istole.it%3A6969&tr=udp%3A%2F%2Fopen.demonii.com%3A1337";
            //magnetLink = "magnet:?xt=urn:btih:737d38ed01da1df727a3e0521a6f2c457cb812de&dn=HOME+-+a+film+by+Yann+Arthus-Bertrand+%282009%29+%5BEnglish%5D+%5BHD+MP4%5D&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.zer0day.to%3A1337&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969";
            //magnetLink = "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c";
            magnetLink = "magnet:?xt=urn:btih:209c8226b299b308beaf2b9cd3fb49212dbd13ec&dn=Tears+of+Steel&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F&xs=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2Ftears-of-steel.torrent"
            //magnetLink = "magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F&xs=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2Fbig-buck-bunny.torrent";
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
                        //System.out.println("Torrent added");
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
                        Log.i(
                            "personal",
                            java.lang.Long.toString(s.stats().totalDownload())
                        )
                    }
                    AlertType.TORRENT_FINISHED -> {
                        Log.i("personal", "Torrent finished")
                        printToast("Torrent downloaded!!")
                        signal.countDown()
                    }
                }

            }

        });

        s.start(params)

        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val nodes = s.stats().dhtNodes()
                // wait for at least 10 nodes in the DHT.
                if (nodes >= 10) {
                    Log.i("personal", "DHT contains $nodes nodes")
                    //signal.countDown();
                    timer.cancel()
                }
            }
        }, 0, 1000)

        printToast("Starting download, please wait...")

        Log.i("personal", "Fetching the magnet uri, please wait...")
        val data = s.fetchMagnet(magnetLink, 30)

        if (data != null) {
            val torrentInfo =
                Entry.bdecode(data).toString()
            Log.i("personal", torrentInfo)
            torrentView.text = torrentInfo

            val ti = TorrentInfo.bdecode(data)
            s.download(ti, File("/storage/emulated/0"))
        } else {
            Log.i("personal", "Failed to retrieve the magnet")
            printToast("Something went wrong, check logs")
        }

    }

}
