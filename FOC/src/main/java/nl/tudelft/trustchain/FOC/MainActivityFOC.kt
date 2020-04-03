package nl.tudelft.trustchain.FOC

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main_foc.*
import kotlinx.android.synthetic.main.content_main_activity_foc.*
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.FOC.databinding.BlankFragmentBinding
import nl.tudelft.trustchain.common.DemoCommunity
import java.util.*
import kotlin.math.roundToInt


class MainActivityFOC : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding = BlankFragmentBinding.inflate(layoutInflater)
        //val view = binding.root
        //setContentView(view)
        setContentView(R.layout.activity_main_foc)
        setSupportActionBar(toolbar)

        Toast.makeText(applicationContext, "No magnet link provided, using default one...", Toast.LENGTH_LONG).show()

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
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

}
