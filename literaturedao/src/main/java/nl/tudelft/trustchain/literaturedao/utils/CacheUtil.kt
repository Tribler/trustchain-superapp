package nl.tudelft.trustchain.literaturedao.utils

import android.content.Context
import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.literaturedao.controllers.QueryHandler
import nl.tudelft.trustchain.literaturedao.data_types.Literature
import nl.tudelft.trustchain.literaturedao.data_types.LocalData
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.util.*

class CacheUtil(val context: Context?)
{
    fun loadLocalData(): LocalData {
        var fileInputStream: FileInputStream? = null

        try{
            fileInputStream = context?.openFileInput("localData.json")
        } catch (e: FileNotFoundException){
            context?.openFileOutput("localData.json", Context.MODE_PRIVATE).use { output ->
                output?.write(Json.encodeToString(LocalData(mutableListOf<Literature>())).toByteArray())
            }
            fileInputStream = context?.openFileInput("localData.json")
        }
        var inputStreamReader: InputStreamReader = InputStreamReader(fileInputStream)
        val bufferedReader: BufferedReader = BufferedReader(inputStreamReader)
        val stringBuilder: StringBuilder = StringBuilder()
        var text: String? = null
        while ({ text = bufferedReader.readLine(); text }() != null) {
            stringBuilder.append(text)
        }
        val localData: LocalData =  Json.decodeFromString<LocalData>(stringBuilder.toString())
        return localData
    }

    fun writeLocalData(localData: LocalData){
        context?.openFileOutput("localData.json", Context.MODE_PRIVATE).use { output ->
            output?.write(Json.encodeToString(localData).toByteArray())
        }
    }

    public fun  localSearch(inp: String): MutableList<Pair<Literature, Double>>{
        var handler = QueryHandler()
        return handler.scoreList(inp, loadLocalData().content)
    }

    fun addLocalData(title: String, magnet: String, kwsi: MutableList<Pair<String, Double>>?, local: Boolean, uri: String){

        var kws = kwsi
        if( kws == null){
            kws = mutableListOf<Pair<String, Double>>()
        }

        val literatureObject = Literature(
            title,
            magnet.toString(),
            kws,
            local,
            Calendar.getInstance().getTime().toString(),
            uri)

        val localData = loadLocalData()

        localData.content.add(literatureObject)

        writeLocalData(localData)
    }
}
