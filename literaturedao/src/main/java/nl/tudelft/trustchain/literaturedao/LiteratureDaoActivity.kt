package nl.tudelft.trustchain.literaturedao
import nl.tudelft.trustchain.common.BaseActivity

import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import nl.tudelft.trustchain.literaturedao.controllers.KeywordExtractor
import nl.tudelft.trustchain.literaturedao.controllers.PdfController
import java.io.InputStream

open class LiteratureDaoActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_literaturedao
    override val bottomNavigationMenu = R.menu.literature_navigation_menu

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
