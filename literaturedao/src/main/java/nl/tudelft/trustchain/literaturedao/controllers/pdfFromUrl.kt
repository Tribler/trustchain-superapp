package nl.tudelft.trustchain.literaturedao.controllers

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import nl.tudelft.trustchain.literaturedao.LiteratureDaoActivity
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class pdfFromUrl(context: LiteratureDaoActivity) {
    val parentContext = context
    private val looperThread = LooperThread(parentContext.baseContext)

    fun startThread(){
        looperThread.start()
    }

    fun stopThread(){
        val looper = looperThread.looper()
        if (looper != null){
            looper.quit()
        }
    }

    fun download(url: String){
        val msg = Message.obtain()
        msg.obj = url
        looperThread.handler?.sendMessage(msg)
    }

    internal class LooperThread(val context: Context) : Thread() {
        private val MEGABYTE = 1024 * 1024
        var handler: Handler? = null
        override fun run() {
            Looper.prepare()
            handler = object : Handler(Looper.myLooper()) {
                override fun handleMessage(msg: Message?) {
                    if( msg?.obj is String){
                        val url = msg?.obj.toString()
                        if (urlCheck(url)){
                            downloadFile(url)
                            printToast("done")
                        }
                    }
                }
            }
            Looper.loop()
        }

        fun looper(): Looper? {
            return Looper.myLooper()
        }
        internal fun urlCheck(url: String): Boolean{
            return URLUtil.isValidUrl(url)
        }

        internal fun downloadFile(url: String?): File{
            var result = File.createTempFile("test", ".pdf", context.getCacheDir())
            try {
                val url = URL(url)
                val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
                urlConnection.connect()
                val inputStream: InputStream = urlConnection.getInputStream()
                val fileOutputStream = FileOutputStream(result)
                val totalSize: Int = urlConnection.getContentLength()
                val buffer = ByteArray(MEGABYTE)
                var bufferLength = 0
                while (inputStream.read(buffer).also { bufferLength = it } > 0) {
                    fileOutputStream.write(buffer, 0, bufferLength)
                }
                fileOutputStream.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return result
        }
        private fun printToast(s: String) {
            Toast.makeText(context.applicationContext, s, Toast.LENGTH_SHORT).show()
        }
    }


/*
    class urlImputDialogue : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                // Use the Builder class for convenient dialog construction
                // 1. Instantiate an <code><a href="/reference/android/app/AlertDialog.Builder.html">AlertDialog.Builder</a></code> with its constructor
                val builder: AlertDialog.Builder? = activity?.let {
                    AlertDialog.Builder(it)
                }
                // 2. Chain together various setter methods to set the dialog characteristics
                builder?.setMessage("Please insert the URL")
                builder?.setTitle("Import PDF from URL")
                // Create the AlertDialog object and return it
                builder?.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }

    }*/
}
