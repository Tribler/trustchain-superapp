package nl.tudelft.trustchain.FOC

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.swig.*
import kotlinx.android.synthetic.main.activity_main_foc.*
import kotlinx.android.synthetic.main.fragment_download.*
import java.io.*
import java.util.*

class MainActivityFOC : AppCompatActivity() {

    private var torrentList = ArrayList<Button>()
    private var progressVisible = false
    private var requestCode = 1
    val MY_PERMISSIONS_REQUEST = 0
    val s = SessionManager()

    private lateinit var appGossiper: AppGossiper

    @Suppress("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main_foc)
            setSupportActionBar(toolbar)
            fab.setOnClickListener { _ ->
                selectNewFileToUpload()
            }

            popUp.visibility = View.GONE
            download_progress.setOnClickListener {
                toggleProgressBar(popUp)
            }

            torrentCount.text = getString(R.string.torrentCount, torrentList.size)

            // upon launching our activity, we ask for the "Storage" permission
            requestStoragePermission()

            printToast("STARTED")
            showAllFiles()
            appGossiper = AppGossiper.getInstance(s, this)
            appGossiper.start()
        } catch (e: Exception) {
            printToast(e.toString())
        }
    }

    private fun toggleProgressBar(progress: RelativeLayout) {
        if (progressVisible) {
            progress.visibility = View.GONE
        } else {
            progress.visibility = View.VISIBLE
        }
        progressVisible = !progressVisible
    }


    @Suppress("deprecation")
    fun showAllFiles() {
        val files = applicationContext.cacheDir.listFiles()
        if (files != null) {
            // todo improve this such that the whole list is not loaded again every time
            val torrentListView = findViewById<LinearLayout>(R.id.torrentList)
            torrentListView.removeAllViews()
            torrentList.clear()
            for (file in files) {
                if (getFileName(file.toUri()).endsWith(".apk")) {
                    createSuccessfulTorrentButton(file.toUri())
                }
            }
        }

        // upon launching our activity, we ask for the "Storage" permission
        requestStoragePermission()
    }

    // change if you want to write to the actual phone storage (needs "write" permission)
    fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) // READ_EXTERNAL_STORAGE
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), // READ_EXTERNAL_STORAGE
                MY_PERMISSIONS_REQUEST
            )
        }
    }

    /**
     * Display a short message on the screen
     */
    private fun printToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
    }

    fun createSuccessfulTorrentButton(uri: Uri) {
        val torrentListView = findViewById<LinearLayout>(R.id.torrentList)
        val button = Button(this)
        val fileName = getFileName(uri)
        button.text = fileName
        button.isAllCaps = false
        button.backgroundTintList = ColorStateList.valueOf( ContextCompat.getColor(applicationContext, R.color.android_green))
        torrentList.add(button)
        torrentCount.text = getString(R.string.torrentCount, torrentList.size)
        button.setOnClickListener {
            loadDynamicCode(fileName)
        }
        button.setOnLongClickListener {
            createTorrent(fileName)
            true
        }
        torrentListView.addView(button)
    }

    fun createUnsuccessfulTorrentButton(torrentName: String) {
        val torrentListView = findViewById<LinearLayout>(R.id.torrentList)
        val button = Button(this)
        button.text = torrentName
        button.isAllCaps = false
        button.backgroundTintList = ColorStateList.valueOf( ContextCompat.getColor(applicationContext, R.color.red))
        torrentListView.addView(button)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == this.requestCode && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return
            }
            val fileName = getFileName(data.data!!)
            try {
                printToast(data.data!!.path!!.split(":").last())
                File(
                    Environment.getExternalStorageDirectory().absolutePath + "/" + data.data!!.path!!.split(":").last()
                ).copyTo(File(applicationContext.cacheDir.absolutePath + "/" + fileName))
            } catch (e: Exception) {
                printToast(e.toString())
                printToast("$fileName already exists!")
                return
            }
            createSuccessfulTorrentButton(data.data!!)
        }
    }

    @Suppress("deprecation")
    fun loadDynamicCode(fileName: String) {
        try {
            val intent = Intent(this, ExecutionActivity::class.java)
            intent.putExtra(
                "fileName",
                "${applicationContext.cacheDir}/${fileName.split("/").last()}"
            )
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFileName(uri: Uri): String {
        var result: String? = null
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        } finally {
            cursor?.close()
        }

        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }


    /**
     * Creates a torrent from a file given as input
     * The extension of the file must be included (for example, .png)
     */
    @Suppress("deprecation")
    fun createTorrent(fileName: String) {
        val file = File(applicationContext.cacheDir.absolutePath + "/" + fileName.split("/").last())
        if (!file.exists()) {
            runOnUiThread { printToast("Something went wrong, check logs") }
            Log.i("personal", "File doesn't exist!")
            return
        }

        val fs = file_storage()
        val l1: add_files_listener = object : add_files_listener() {
            override fun pred(p: String): Boolean {
                return true
            }
        }
        libtorrent.add_files_ex(fs, file.absolutePath, l1, create_flags_t())
        val ct = create_torrent(fs)
        val l2: set_piece_hashes_listener = object : set_piece_hashes_listener() {
            override fun progress(i: Int) {}
        }

        val ec = error_code()
        libtorrent.set_piece_hashes_ex(ct, file.parent, l2, ec)
        val torrent = ct.generate()
        val buffer = torrent.bencode()

        val torrentName = fileName.substringBeforeLast('.') + ".torrent"

        var os: OutputStream? = null
        try {
            os = FileOutputStream(File(applicationContext.cacheDir, torrentName.split("/").last()))
            os.write(Vectors.byte_vector2bytes(buffer), 0, Vectors.byte_vector2bytes(buffer).size)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                os!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        val ti = TorrentInfo.bdecode(Vectors.byte_vector2bytes(buffer))
        val magnetLink = "magnet:?xt=urn:btih:" + ti.infoHash() + "&dn=" + ti.name()
        Log.i("personal", magnetLink)
        runOnUiThread { printToast(fileName) }
    }

    @Suppress("deprecation")
    fun selectNewFileToUpload() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        startActivityForResult(intent, requestCode)
    }
}
