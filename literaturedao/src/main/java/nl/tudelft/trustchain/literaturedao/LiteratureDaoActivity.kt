package nl.tudelft.trustchain.literaturedao
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.literaturedao.controllers.KeywordExtractor
import nl.tudelft.trustchain.literaturedao.controllers.PdfController
import java.io.InputStream

open class LiteratureDaoActivity : BaseActivity() {
    override val navigationGraph: Int = R.navigation.nav_literaturedao
    override val bottomNavigationMenu: Int
        get() = super.bottomNavigationMenu

    override fun onStart() {
        super.onStart()
        Log.e("litdao", "starting ...")
        PDFBoxResourceLoader.init(getApplicationContext());
        var pdfController = PdfController()
        var i = 1
        while (i < 2){
            val stream: InputStream = getAssets().open(i.toString() + ".pdf")
            val result = KeywordExtractor()
                .actualImplementation(pdfController
                    .stripText(stream))
                .toString()
            /*val result = KeywordExtractor()
                .quikFix(pdfController
                    .stripText(stream))
                .toString()*/
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
