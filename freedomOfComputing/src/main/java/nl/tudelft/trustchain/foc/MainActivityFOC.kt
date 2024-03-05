package nl.tudelft.trustchain.foc

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.Vectors
import com.frostwire.jlibtorrent.swig.add_files_listener
import com.frostwire.jlibtorrent.swig.create_flags_t
import com.frostwire.jlibtorrent.swig.create_torrent
import com.frostwire.jlibtorrent.swig.error_code
import com.frostwire.jlibtorrent.swig.file_storage
import com.frostwire.jlibtorrent.swig.libtorrent
import com.frostwire.jlibtorrent.swig.set_piece_hashes_listener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.foc.community.FOCCommunity
import nl.tudelft.trustchain.foc.databinding.ActivityMainFocBinding
import nl.tudelft.trustchain.foc.util.ExtensionUtils.Companion.APK_DOT_EXTENSION
import nl.tudelft.trustchain.foc.util.ExtensionUtils.Companion.TORRENT_DOT_EXTENSION
import nl.tudelft.trustchain.foc.util.MagnetUtils.Companion.DISPLAY_NAME_APPENDER
import nl.tudelft.trustchain.foc.util.MagnetUtils.Companion.PRE_HASH_STRING
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URL
import java.net.URLConnection

const val CONNECTION_TIMEOUT: Int = 10000
const val READ_TIMEOUT: Int = 5000

const val DEFAULT_APK = "search.apk"

open class MainActivityFOC : AppCompatActivity() {
    lateinit var binding: ActivityMainFocBinding

    private val scope = CoroutineScope(Dispatchers.IO)
    var torrentList = ArrayList<Button>()
    private var progressVisible = false
    private var debugVisible = false
    private var bufferSize = 1024 * 5
    private val s = SessionManager()
    private var torrentAmount = 0

    private var appGossiper: AppGossiper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainFocBinding.inflate(layoutInflater)
        val view = binding.root

        try {
            setContentView(view)

            setSupportActionBar(binding.toolbar)

            binding.fab.setOnClickListener {
                createDownloadDialog()
            }

            binding.searchBarInput.setOnClickListener {
                val search = binding.searchBarInput.text.toString()
                searchAllFiles(search)
            }

            hidePopUps(binding.popUpLayout.popUp, binding.debugLayout.debugPopUp)
            binding.downloadProgress.setOnClickListener {
                toggleProgressBar(binding.popUpLayout.popUp)
            }

            binding.debugPopUpButton.setOnClickListener {
                toggleDebugPopUp(binding.debugLayout.debugPopUp)
            }

            binding.torrentCount.text = getString(R.string.torrentCount, torrentAmount)

            copyDefaultApp()
            showAllFiles()

            appGossiper =
                IPv8Android.getInstance().getOverlay<FOCCommunity>()
                    ?.let { AppGossiper.getInstance(s, this, it) }
            appGossiper?.start()
        } catch (e: Exception) {
            printToast(e.toString())
        }
    }

    // TODO: Remove hacky fix.
    private fun hidePopUps(
        popUpLayout: RelativeLayout,
        debugLayout: LinearLayout
    ) {
        popUpLayout.visibility = View.GONE
        debugLayout.visibility = View.GONE
        binding.popUpLayout.popUp.visibility = View.GONE
        binding.debugLayout.debugPopUp.visibility = View.GONE
    }

    private fun toggleDebugPopUp(layout: LinearLayout) {
        if (debugVisible) {
            layout.visibility = View.GONE
        } else {
            layout.visibility = View.VISIBLE
        }

        if (progressVisible) {
            progressVisible = false
            binding.popUpLayout.popUp.visibility = View.GONE
        }
        debugVisible = !debugVisible
    }

    private fun toggleProgressBar(progress: RelativeLayout) {
        if (progressVisible) {
            progress.visibility = View.GONE
        } else {
            progress.visibility = View.VISIBLE
        }
        if (debugVisible) {
            debugVisible = false
            binding.debugLayout.debugPopUp.visibility = View.GONE
        }
        progressVisible = !progressVisible
    }

    override fun onResume() {
        super.onResume()
        resumeUISettings()
        appGossiper?.resume()
    }

    override fun onPause() {
        super.onPause()
        appGossiper?.pause()
    }

    private fun showAllFiles() {
        val files = applicationContext.cacheDir.listFiles()
        if (files != null) {
            val torrentListView = binding.contentMainActivityFocLayout.torrentList
            torrentListView.removeAllViews()
            torrentList.clear()
            for (file in files) {
                if (getFileName(file.toUri()).endsWith(APK_DOT_EXTENSION)) {
                    createSuccessfulTorrentButton(file.toUri())
                }
            }
        }
    }

    fun showAddedFile(torrentName: String) {
        val files = applicationContext.cacheDir.listFiles()
        if (files != null) {
            val file =
                files.find { file ->
                    getFileName(file.toUri()) == torrentName
                }
            if (file != null) {
                createSuccessfulTorrentButton(file.toUri())
            }
        }
    }

    private fun searchAllFiles(searchVal: String) {
        val torrentListView = binding.contentMainActivityFocLayout.torrentList
        torrentListView.removeAllViews()
        torrentList.clear()
        val files = applicationContext.cacheDir.listFiles()
        if (files != null) {
            for (file in files) {
                val fileName = getFileName(file.toUri())
                if (fileName.endsWith(APK_DOT_EXTENSION) && fileName.contains(searchVal)) {
                    createSuccessfulTorrentButton(file.toUri())
                }
            }
        }
    }

    /**
     * Ensures that there will always be one apk runnable from within FoC.
     */
    private fun copyDefaultApp() {
        try {
            val file = File(this.applicationContext.cacheDir.absolutePath + "/" + DEFAULT_APK)
            if (!file.exists()) {
                val outputStream = FileOutputStream(file)
                val ins =
                    resources.openRawResource(
                        resources.getIdentifier(
                            DEFAULT_APK.split('.').first(),
                            "raw",
                            packageName
                        )
                    )
                outputStream.write(ins.readBytes())
                ins.close()
                outputStream.close()
                this.createTorrent(DEFAULT_APK)
            }
        } catch (e: Exception) {
            this.printToast(e.toString())
        }
    }

    /**
     * Display a short message on the screen
     */
    private fun printToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_SHORT).show()
    }

    private fun createSuccessfulTorrentButton(uri: Uri) {
        val torrentListView = binding.contentMainActivityFocLayout.torrentList
        var button = Button(this)
        val fileName = getFileName(uri)
        button.text = fileName
        // Replace the failed torrent with the downloaded torrent
        val existingButton = torrentList.find { btn -> btn.text == fileName }
        if (existingButton != null) {
            button = existingButton
        } else {
            torrentList.add(button)
            torrentListView.addView(button)
        }

        button.isAllCaps = false
        button.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(applicationContext, R.color.blue))
        button.setTextColor(ContextCompat.getColor(applicationContext, R.color.white))
        binding.torrentCount.text = getString(R.string.torrentCount, ++torrentAmount)
        button.setOnClickListener {
            loadDynamicCode(fileName)
        }
        button.setOnLongClickListener {
            createAlertDialog(fileName)
            true
        }
    }

    fun createUnsuccessfulTorrentButton(torrentName: String) {
        val torrentListView = binding.contentMainActivityFocLayout.torrentList
        val button = Button(this)
        button.text = torrentName
        torrentList.add(button)
        torrentListView.addView(button)
        button.isAllCaps = false
    }

    private fun createAlertDialog(fileName: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.createAlertDialogTitle))
        builder.setMessage(getString(R.string.createAlertDialogMsg))
        builder.setPositiveButton(getString(R.string.cancelButton), null)
        builder.setNeutralButton(getString(R.string.deleteButton)) { _, _ -> deleteApkFile(fileName) }
        builder.setNegativeButton(getString(R.string.createButton)) { _, _ -> createTorrent(fileName) }
        builder.show()
    }

    private fun createDownloadDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.createDownloadDialogTitle))
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setSingleLine()
        val container = FrameLayout(this)
        val params =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        params.leftMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
        params.rightMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)
        builder.setNegativeButton(getString(R.string.cancelButton), null)
        builder.setPositiveButton(getString(R.string.downloadButton)) { _, _ ->
            scope.launch {
                selectNewUrlToDownload(
                    input.text.toString()
                )
            }
        }
        builder.show()
    }

    private fun deleteApkFile(fileName: String) {
        val files = applicationContext.cacheDir.listFiles()
        if (files != null) {
            val file =
                files.find { file ->
                    getFileName(file.toUri()) == fileName
                }
            val deleted = file?.delete()

            // delete torrent file if it exists
            files.find { torrentFile ->
                getFileName(torrentFile.toUri()) ==
                    fileName.replace(
                        APK_DOT_EXTENSION,
                        TORRENT_DOT_EXTENSION
                    )
            }?.delete()

            if (deleted != null && deleted) {
                val buttonToBeDeleted = torrentList.find { button -> button.text == fileName }
                if (buttonToBeDeleted != null) {
                    val torrentListView = binding.contentMainActivityFocLayout.torrentList
                    torrentList.remove(buttonToBeDeleted)
                    torrentListView.removeView(buttonToBeDeleted)
                    binding.torrentCount.text = getString(R.string.torrentCount, --torrentAmount)
                    appGossiper?.removeTorrent(fileName)
                }
            }
        }
    }

    private fun loadDynamicCode(fileName: String) {
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

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                result =
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
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
    fun createTorrent(fileName: String): TorrentInfo? {
        val file = File(applicationContext.cacheDir.absolutePath + "/" + fileName.split("/").last())
        if (!file.exists()) {
            runOnUiThread { printToast("Something went wrong, check logs") }
            Log.i("personal", "File doesn't exist!")
            return null
        }

        val fs = file_storage()
        val l1: add_files_listener =
            object : add_files_listener() {
                override fun pred(p: String): Boolean {
                    return true
                }
            }
        libtorrent.add_files_ex(fs, file.absolutePath, l1, create_flags_t())
        val ct = create_torrent(fs)
        val l2: set_piece_hashes_listener =
            object : set_piece_hashes_listener() {
                override fun progress(i: Int) {}
            }

        val ec = error_code()
        libtorrent.set_piece_hashes_ex(ct, file.parent, l2, ec)
        val torrent = ct.generate()
        val buffer = torrent.bencode()

        val torrentName = fileName.substringBeforeLast('.') + TORRENT_DOT_EXTENSION

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
        val magnetLink = PRE_HASH_STRING + ti.infoHash() + DISPLAY_NAME_APPENDER + ti.name()
        Log.i("personal", magnetLink)
        runOnUiThread { printToast(fileName) }
        return ti
    }

    private fun selectNewUrlToDownload(urlName: String) {
        if (urlName.trim() == "") {
            this.runOnUiThread { printToast("No URL provided.") }
            return
        }

        val urlTitle = urlName.substringAfterLast('/')
        val url = URL(urlName)
        val filePath: String = this.applicationContext.cacheDir.absolutePath + "/" + urlTitle

        try {
            val file = File(filePath)

            if (file.exists()) {
                deleteApkFile(filePath)
                this.runOnUiThread { printToast("Replacing existing APK with the same name.") }
            }

            val con: URLConnection = url.openConnection()
            con.connectTimeout = CONNECTION_TIMEOUT
            con.readTimeout = READ_TIMEOUT

            val inStream = BufferedInputStream(con.getInputStream(), this.bufferSize)

            file.createNewFile()

            val outStream = FileOutputStream(file)

            // Ensure the apk file is read only see:
            // https://developer.android.com/about/versions/14/behavior-changes-14#safer-dynamic-code-loading
            file.setReadOnly()

            val buff = ByteArray(this.bufferSize)

            var len: Int
            while (inStream.read(buff).also { len = it } != -1) {
                outStream.write(buff, 0, len)
            }

            outStream.flush()
            outStream.close()
            inStream.close()
            this.runOnUiThread { showAllFiles() }
        } catch (e: Exception) {
            this.runOnUiThread { printToast(e.toString()) }
        }
    }

    private fun resumeUISettings() {
        binding.downloadCount.text =
            getString(R.string.downloadsInProgress, appGossiper?.downloadsInProgress)
        binding.popUpLayout.inQueue.text =
            getString(
                R.string.downloadsInQueue,
                kotlin.math.max(0, appGossiper?.downloadsInProgress?.minus(1) ?: 0)
            )
        binding.popUpLayout.currentDownload.text = appGossiper?.currentDownloadInProgress
        binding.popUpLayout.progressBar.progress = 0
        binding.popUpLayout.progressBarPercentage.text =
            getString(R.string.downloadProgressPercentage, "0%")
        binding.debugLayout.evaRetryCounter.text =
            getString(R.string.evaRetries, appGossiper?.evaRetries)
        binding.debugLayout.failedCounter.text =
            getString(R.string.failedCounter, appGossiper?.failedTorrents.toString())
    }
}
