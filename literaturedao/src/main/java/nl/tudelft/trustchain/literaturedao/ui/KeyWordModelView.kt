package nl.tudelft.trustchain.literaturedao.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.literaturedao.controllers.KeywordExtractor
import nl.tudelft.trustchain.literaturedao.controllers.PdfController
import java.io.InputStream


class KeyWordModelView(context: Context) : ViewModel() {

    val context = context

    fun calcKWs(path: String){
        viewModelScope.launch{
            Log.e("litdao", "litdao: Coroutine launched")
            try{
                operations(path)
            } catch (e: Exception) {
                Log.e("litdao", "litdao: " + e.toString())
            }
        }
    }

    @Serializable
    data class Data(val content: MutableList<Pair<String, Double>>)

    fun operations(path: String){
        PDFBoxResourceLoader.init(context)
        val csv: InputStream = context.getAssets().open("stemmed_freqs.csv")
        val pdf: InputStream = context.getAssets().open(path)
        val strippedString = PdfController().stripText(pdf)
        val kws = KeywordExtractor().extract(strippedString, csv)
        Log.e("litdao", "litdao: " + kws.toString())
        context.openFileOutput(path, Context.MODE_PRIVATE).use { output ->
            output.write(Json.encodeToString(Data(kws)).toByteArray())
        }
    }
}
