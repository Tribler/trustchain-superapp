package nl.tudelft.trustchain.literaturedao
import android.os.Bundle
import nl.tudelft.trustchain.common.BaseActivity

import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.literaturedao.controllers.KeywordExtractor
import nl.tudelft.trustchain.literaturedao.controllers.PdfController
import nl.tudelft.trustchain.literaturedao.ipv8.LiteratureCommunity
import java.io.InputStream
import java.util.*
import kotlin.math.roundToInt

open class LiteratureDaoActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_literaturedao
    override val bottomNavigationMenu = R.menu.literature_navigation_menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val literatureCommunity = IPv8Android.getInstance().getOverlay<LiteratureCommunity>()!!
        printPeersInfo(literatureCommunity)
        Log.i("litdao","I am "+literatureCommunity.myPeer.mid+ "and Im broadcasting: hello")
        literatureCommunity.broadcastDebugMessage("hello")
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

    override fun onStart() {
        super.onStart()
        Log.e("litdao", "starting ...")
        PDFBoxResourceLoader.init(getApplicationContext());
        var pdfController = PdfController()
        var i = 1
        while (i < 2){
            val stream: InputStream = getAssets().open(i.toString() + ".pdf")
            //val csv: InputStream = getAssets().open("stemmed_freqs.csv")
            //val result: String
            val result = KeywordExtractor()
                .quikFix(pdfController
                    .stripText(stream))
                .toString()
            /*
            if (csv != null){
                val reader = BufferedReader(InputStreamReader(csv))
                val result = KeywordExtractor()
                    .actualImplementation(pdfController
                        .stripText(stream), reader)
                    .toString()
            } else {
                val result = KeywordExtractor()
                    .quikFix(pdfController
                        .stripText(stream))
                    .toString()
            }*/

            Log.e("litdao", "litdao: " + result)
            i += 1
        }
        //Log.d("litdao", pdfController.stripText(stream))

/*
        Snackbar sb = Snackbar.make(findViewById(R.id.linearLayout), R.string.offline_message, Snackbar.LENGTH);
        snackbar.show
        ()
*/
        //Toast.makeText(LiteratureDaoActivity(), result, Toast.LENGTH_SHORT).show();
    }
}
