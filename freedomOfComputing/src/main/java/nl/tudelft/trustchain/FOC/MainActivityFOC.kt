package nl.tudelft.trustchain.FOC

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.frostwire.jlibtorrent.*
import kotlinx.android.synthetic.main.activity_main_foc.*
import kotlinx.android.synthetic.main.fragment_download.*
import java.util.*

class MainActivityFOC : AppCompatActivity() {

    private var torrentList = ArrayList<Button>()
    private var progressVisible = false
    private var requestCode = 1
    val MY_PERMISSIONS_REQUEST = 0
    var downloadsInProgress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_foc)
        setSupportActionBar(toolbar)
        fab.setOnClickListener { _ ->
            selectNewFileToUpload()
        }

        popUp.visibility = View.GONE
        download_progress.setOnClickListener{
            toggleProgressBar(popUp)
        }

        download_count.text = getString(R.string.downloadsInProgress, downloadsInProgress)
        torrentCount.text = getString(R.string.torrentCount, torrentList.size)

        // upon launching our activity, we ask for the "Storage" permission
        requestStoragePermission()
        printToast("STARTED")
    }

    private fun toggleProgressBar(progress: RelativeLayout) {
        if (progressVisible) {
            progress.visibility = View.GONE
        } else {
            progress.visibility = View.VISIBLE
        }
        progressVisible = !progressVisible
    }

    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val torrentListView = findViewById<LinearLayout>(R.id.torrentList)

        if (requestCode == this.requestCode && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return
            }
            // Create a new button
            val button = Button(this)
            val fileName = getFileName(data.data!!)
            button.text = fileName
            button.isAllCaps = false
            torrentListView.addView(button)
            torrentList.add(button)
            torrentCount.text = getString(R.string.torrentCount, torrentList.size)
            button.setOnClickListener {
                loadDynamicCode(fileName)
            }

            printToast("File uploaded!")
        }
    }

    @Suppress("deprecation")
    fun loadDynamicCode(fileName: String) {
        try {
            val intent = Intent(this, ExecutionActivity::class.java)
            intent.putExtra(
                "fileName",
                Environment.getExternalStorageDirectory().absolutePath + "/Download/" + fileName
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

    @Suppress("deprecation")
    fun selectNewFileToUpload() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        startActivityForResult(intent, requestCode)
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
    fun printToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
    }
}
