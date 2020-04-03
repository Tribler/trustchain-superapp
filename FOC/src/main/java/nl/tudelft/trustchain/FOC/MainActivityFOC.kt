package nl.tudelft.trustchain.FOC

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main_foc.*
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

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
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


    companion object {
        private lateinit var binding: BlankFragmentBinding

        @JvmStatic
        fun setBinding(bind: BlankFragmentBinding) {
            binding = bind
        }

        fun debug(bind: BlankFragmentBinding) {
            bind.progressBar.setProgress(70, true);
        }


    }


    //a
    //blabla



}
