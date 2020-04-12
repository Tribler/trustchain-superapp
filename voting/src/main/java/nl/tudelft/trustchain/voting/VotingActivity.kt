package nl.tudelft.trustchain.voting

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main_voting.*

class VotingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_voting)
//        setSupportActionBar(toolbar)

        printToast("STARTED")
//
//        // option 1: download a torrent through a magnet link
//        downloadMagnetButton.setOnClickListener { _ ->
//            getMagnetLink()
//        }
//
//        // option 2: download a torrent through a .torrent file on your phone
//        downloadTorrentButton.setOnClickListener { _ ->
//            getTorrent()
//        }
//
//        // option 3: Send a message to every other peer using the superapp
//        greetPeersButton.setOnClickListener { _ ->
//            val ipv8 = IPv8Android.getInstance()
//            val demoCommunity = ipv8.getOverlay<DemoCommunity>()!!
//            val peers = demoCommunity.getPeers()
//
//            Log.i("personal", "n:" + peers.size.toString())
//            for (peer in peers) {
//                Log.i("personal", peer.mid)
//            }
//
//            demoCommunity.broadcastGreeting()
//            printToast("Greeted " + peers.size.toString() + " peers")
//        }
//
//        // option 4: dynamically load and execute code from a jar/apk file
//        executeCodeButton.setOnClickListener { _ ->
//            loadDynamicCode()
//        }
//
//        // upon launching our activity, we ask for the "Storage" permission
//        requestStoragePermission()
    }

    /**
     * Display a short message on the screen
     */
    fun printToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
    }

}
