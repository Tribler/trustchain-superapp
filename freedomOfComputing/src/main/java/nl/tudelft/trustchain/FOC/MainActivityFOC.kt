package nl.tudelft.trustchain.FOC

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
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
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.Vectors
import com.frostwire.jlibtorrent.swig.*
import kotlinx.android.synthetic.main.activity_main_foc.*
import kotlinx.android.synthetic.main.fragment_debugging.*
import kotlinx.android.synthetic.main.fragment_download.*
import java.util.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class MainActivityFOC : AppCompatActivity() {

    private var torrentList = ArrayList<Button>()
    private var progressVisible = false
    private var debugVisible = false
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

            download_progress.setOnClickListener {
                toggleProgressBar(popUp)
            }

            debugPopUpButton.setOnClickListener {
                toggleDebugPopUp(debugPopUp)
            }

            torrentCount.text = getString(R.string.torrentCount, torrentList.size)

            // upon launching our activity, we ask for the "Storage" permission
            requestStoragePermission()

            copyDefaultApp()

            printToast("STARTED")
            showAllFiles()
            appGossiper = AppGossiper.getInstance(s, this)
            appGossiper.start()
        } catch (e: Exception) {
            this.printToast("1")
            printToast(e.toString())
        }
    }

    fun toggleDebugPopUp(layout: LinearLayout) {
        if (debugVisible) layout.visibility = View.GONE
        else layout.visibility = View.VISIBLE

        if (progressVisible) {
            progressVisible = false
            popUp.visibility = View.GONE
        }
        debugVisible = !debugVisible
    }

    private fun toggleProgressBar(progress: RelativeLayout) {
        if (progressVisible) progress.visibility = View.GONE
        else progress.visibility = View.VISIBLE

        if (debugVisible) {
            debugVisible = false
            debugPopUp.visibility = View.GONE
        }
        progressVisible = !progressVisible
    }

    override fun onResume() {
        super.onResume()
        appGossiper.resume()
    }

    override fun onPause() {
        super.onPause()
        appGossiper.pause()
    }


    @Suppress("deprecation")
    fun showAllFiles() {
        val files = applicationContext.cacheDir.listFiles()
        if (files != null) {
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

    fun showAddedFile(torrentName: String) {
        val files = applicationContext.cacheDir.listFiles()
        if (files != null) {
            val file = files.find { file ->
                getFileName(file.toUri()) == torrentName
            }
            if (file != null) {
                createSuccessfulTorrentButton(file.toUri())
            }
        }
    }

    /**
     * Ensures that there will always be one apk runnable from within FoC.
     */
    private fun copyDefaultApp() {
        try {
            val file = File(this.applicationContext.cacheDir.absolutePath + "/search.apk")
            if (!file.exists()) {
                val outputStream = FileOutputStream(file)
                val ins = resources.openRawResource(resources.getIdentifier("search", "raw", packageName))
                outputStream.write(ins.readBytes())
                ins.close()
                outputStream.close()
                this.createTorrent("search.apk")
            }
        } catch (e: Exception) {
            this.printToast("2")
            this.printToast(e.toString())
        }
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
        Toast.makeText(applicationContext, s, Toast.LENGTH_SHORT).show()
    }

    fun createSuccessfulTorrentButton(uri: Uri) {
        val torrentListView = findViewById<LinearLayout>(R.id.torrentList)
        var button = Button(this)
        val fileName = getFileName(uri)
        button.text = fileName
        // Replace the failed torrent with the downloaded torrent
        val existingButton = torrentList.find { btn -> btn.text == fileName }
        if (existingButton != null) {
            button = existingButton;
        } else {
            torrentList.add(button)
            torrentListView.addView(button)
        }

        button.isAllCaps = false
        button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(applicationContext, R.color.blue))
        button.setTextColor(ContextCompat.getColor(applicationContext, R.color.white))
        torrentCount.text = getString(R.string.torrentCount, torrentList.size)
        button.setOnClickListener {
            loadDynamicCode(fileName)
        }
        button.setOnLongClickListener {
            createAlertDialog(fileName)
            true
        }
    }


    fun createAlertDialog(fileName: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Create or Delete")
        builder.setMessage("Select whether you want to delete the apk or create a torrent out of it")
        builder.setPositiveButton("Cancel", null)
        builder.setNeutralButton("Delete") { _, _ -> deleteApkFile(fileName)}
        builder.setNegativeButton("Create") { _, _ -> createTorrent(fileName)}
        builder.show()
    }

    fun deleteApkFile(fileName: String) {
        val files = applicationContext.cacheDir.listFiles()
        if (files != null) {
            val file = files.find { file ->
                getFileName(file.toUri()) == fileName
            }
            val deleted = file?.delete()

            // delete torrent file if it exists
            files.find { torrentFile ->
                getFileName(torrentFile.toUri()) == fileName.replace(".apk", ".torrent")
            }?.delete()

            if (deleted != null && deleted) {
                val buttonToBeDeleted = torrentList.find { button -> button.text == fileName }
                if (buttonToBeDeleted != null) {
                    val torrentListView = findViewById<LinearLayout>(R.id.torrentList)
                    torrentList.remove(buttonToBeDeleted)
                    torrentListView.removeView(buttonToBeDeleted)
                    torrentCount.text = getString(R.string.torrentCount, torrentList.size)
                    appGossiper.removeTorrent(fileName)
                }
            }
        }
    }

    fun createUnsuccessfulTorrentButton(torrentName: String) {
        // No need to create duplicate failed torrent buttons
        val existingButton = torrentList.find { btn -> btn.text == torrentName }
        if (existingButton == null) {
            val torrentListView = findViewById<LinearLayout>(R.id.torrentList)
            val button = Button(this)
            button.text = torrentName
            button.isAllCaps = false
            torrentList.add(button)
            torrentListView.addView(button)
        }
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
//                    Environment.getExternalStorageDirectory().absolutePath + "/" + data.data!!.path!!.split(":").last()
                    Environment.getExternalStorageDirectory().absolutePath + "/" + fileName
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
    fun createTorrent(fileName: String): TorrentInfo? {
        val file = File(applicationContext.cacheDir.absolutePath + "/" + fileName.split("/").last())
        if (!file.exists()) {
            runOnUiThread { printToast("Something went wrong, check logs") }
            Log.i("personal", "File doesn't exist!")
            return null
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
        return ti
    }

    @Suppress("deprecation")
    fun selectNewFileToUpload() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        startActivityForResult(intent, requestCode)
    }
}
