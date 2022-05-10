package nl.tudelft.trustchain.literaturedao.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.literaturedao.LiteratureDaoActivity
import nl.tudelft.trustchain.literaturedao.controllers.KeywordExtractor
import nl.tudelft.trustchain.literaturedao.controllers.PdfController
import java.io.InputStream


class KeyWordModelView(context: LiteratureDaoActivity) : ViewModel() {

    val context = context

    fun calcKWs(path: String){
        viewModelScope.launch(Dispatchers.Default){
            Log.e("litdao", "litdao: Coroutine launched")
            try{
                operations(path)
            } catch (e: Exception) {
                Log.e("litdao", "litdao: " + e.toString())
            }
        }
    }

    @Serializable
    data class Data(val content: MutableList<Pair<String, MutableList<Pair<String, Double>>>>)

    suspend fun operations(path: String) = withContext(Dispatchers.Default){
        PDFBoxResourceLoader.init(context)
        val csv: InputStream = context.getAssets().open("stemmed_freqs.csv")
        val pdf: InputStream = context.getAssets().open(path)
        val strippedString = PdfController().stripText(pdf)
        val kws = KeywordExtractor().extract(strippedString, csv)
        Log.e("litdao", "newWrite: " + kws.toString())
        context.metaDataLock.lock()
        var metadata = context.loadMetaData()
        metadata.content.add(Pair(path, kws))
        context.writeMetaData(metadata)
        context.metaDataLock.unlock()
    }
}
