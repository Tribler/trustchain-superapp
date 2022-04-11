package nl.tudelft.trustchain.frost

import android.content.Context
import android.util.Log

fun writeToFile(context: Context?, filePath: String, text: String){
    context?.openFileOutput(filePath, Context.MODE_PRIVATE).use {
        it?.write(text.toByteArray())
        Log.i("FROST", "Write: $text to $filePath")
    }
}

fun readFile(context: Context?, filePath: String): String?{
    context?.openFileInput(filePath).use { stream ->
        val text = stream?.bufferedReader().use {
            it?.readText()
        }
        Log.i("FROST", "Read: $text from $filePath")
        return text
    }
}


