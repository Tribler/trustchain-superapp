package nl.tudelft.trustchain.literaturedao

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.Vectors
import com.frostwire.jlibtorrent.swig.*
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.*
import nl.tudelft.trustchain.literaturedao.controllers.KeywordExtractor
import nl.tudelft.trustchain.literaturedao.utils.ExtensionUtils
import nl.tudelft.trustchain.literaturedao.utils.MagnetUtils
import org.apache.commons.io.FileUtils
import java.io.*
import nl.tudelft.trustchain.literaturedao.data_types.*
import nl.tudelft.trustchain.literaturedao.utils.CacheUtil
import java.util.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit


class AddLiteratureFragment : Fragment(R.layout.fragment_literature_add) {
    private var downloaded = false
    private lateinit var selectedFile: DocumentFile
    private var literatureGossiper: LiteratureGossiper? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view: View = inflater.inflate(R.layout.fragment_literature_add, container, false)
        val selectFileUpload: Button = view.findViewById(R.id.select_new_lirterature) as Button
        val submitFileUpload: Button = view.findViewById(R.id.submit_new_lirterature) as Button
        val urlTextField: EditText = view.findViewById(R.id.url_text_field) as EditText

        //inside Fragment
        val job = Job()
        val uiScope = CoroutineScope(Dispatchers.Main + job)



        selectFileUpload.setOnClickListener {
            // do something
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            @Suppress("DEPRECATION") // TODO: Fix deprecation issue.
            startActivityForResult(intent, 101)
        }

        val clipBoardManager =
            context?.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
        val urlCopiedTest = clipBoardManager.primaryClip?.getItemAt(0)?.text?.toString()

        if (urlCopiedTest != null && URLUtil.isValidUrl(urlCopiedTest))
            urlTextField.setText(urlCopiedTest)

        submitFileUpload.setOnClickListener {
            try {
                //TODO: Start Loading animation and start thread

                uiScope.launch(Dispatchers.IO) {

                    withContext(Dispatchers.Main) {
                        view.findViewById<LinearLayout>(R.id.add_literature_content).visibility =
                            View.GONE
                        view.findViewById<LinearLayout>(R.id.add_literature_loading).visibility =
                            View.VISIBLE
                    }

                    val URLInput = view.findViewById<EditText>(R.id.url_text_field).text.toString()
                    val nameOfLit =
                        view.findViewById<EditText>(R.id.literature_title).text.toString()

                    var pdf: InputStream?

                    if (urlCheck(URLInput)) {
                        val file = downloadFile(nameOfLit, URLInput)
                        pdf = requireContext().contentResolver.openInputStream(Uri.fromFile(file))
                        val docFile =
                            DocumentFile.fromSingleUri(requireContext(), Uri.fromFile(file))
                        if (docFile != null) {
                            selectedFile = docFile
                        }
                        downloaded = true
                    } else {
                        pdf = requireContext().contentResolver.openInputStream(selectedFile.uri)
                    }

                    // TODO: Select where you want to select the file from;
                    // case 1: A file location/URI is selected in selectedFile.uri
                    // case2: A internet URL is selected;
                    // Step 1: Download the file to download directory.
                    //https://medium.com/mobile-app-development-publication/download-file-in-android-with-kotlin-874d50bccaa2
                    //val pdf = requireContext().contentResolver.openInputStream(==== Downlaoded file URI (downloads/pdf...)====)


                    PDFBoxResourceLoader.init(activity?.baseContext)

                    val strippedString = stripText(pdf!!)
                    val kws: MutableList<Pair<String, Double>>

                    // initilaizing freq's
                    val csv: InputStream = context?.getAssets()?.open("stemmed_freqs.csv")
                        ?: throw Exception("Stemmed_freqs.csv not found")

                    // Extract keywords
                    kws = KeywordExtractor().extract(strippedString, csv)

                    // Create Torrent
                    val magnet = createTorrentFromFileUri(requireContext(), selectedFile.uri)
                    if (magnet != null) {
                        literatureGossiper?.addTorrentInfo(magnet)
                    }

                    val literatureTitle = view.findViewById<EditText>(R.id.literature_title).text

                    val literatureObject = Literature(
                        literatureTitle.toString(),
                        magnet.toString(),
                        kws,
                        true,
                        Calendar.getInstance().getTime().toString(),
                        selectedFile.getUri().toString()
                    )


                    val localData = CacheUtil(context).loadLocalData()
                    localData.content.add(literatureObject)
                    CacheUtil(context).writeLocalData(localData)

                    // TODO: Gossip Result
                    // JSON Serialize to string the newLiterature and gossip it to the connected peers.
                    // Comment: This should be a constantly running process that uses the locally stored pdf's
                    // so there should be no need to do anything after we stored it in local storage


                    // TODO: Move to Home Screen
                    withContext(Dispatchers.Main) {
                        view.findViewById<LinearLayout>(R.id.add_literature_loading).visibility =
                            View.GONE
                        view.findViewById<LinearLayout>(R.id.add_literature_done).visibility =
                            View.VISIBLE
                    }
                }

            } catch (ex: Exception) {
                // TODO: Show error
                ex.printStackTrace()
                Log.e("litdao", ex.toString())
            }
        }
        // Inflate the layout for this fragment
        return view
    }

    fun urlCheck(url: String): Boolean {
        return URLUtil.isValidUrl(url)
    }

    internal fun downloadFile(name: String, url: String): File {

        val client = OkHttpClient()
        val MEGABYTE = 1024 * 1024
        val okHttpBuilder = client.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
        okHttpBuilder.build()

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        val body = response.body
        // TODO: refactor this function call.
        @Suppress("DEPRECATION") val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        var file = File(downloadsDir.toString() + File.separator.toString() + name + ".pdf")

        if (body != null) {
            body.byteStream().apply {
                file.outputStream().use { fileOut ->
                    copyTo(fileOut, MEGABYTE)
                }
            }
        } else {
            throw Exception("No body returned in url request.")
        }
        return file
    }

    fun stripText(file: InputStream): String {
        var parsedText = ""
        var document: PDDocument? = null

        try {
            document = PDDocument.load(file)
        } catch (e: IOException) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while loading document to strip", e)
        }

        try {
            val pdfStripper = PDFTextStripper()
            parsedText = pdfStripper.getText(document)
        } catch (e: IOException) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while stripping text", e)
        } finally {
            try {
                document?.close()
            } catch (e: IOException) {
                Log.e("PdfBox-Android-Sample", "Exception thrown while closing document", e)
            }
        }

        return parsedText
    }

    /**
     * Creates a torrent from a file uri in the given context
     */
    fun createTorrentFromFileUri(context: Context, uri: Uri): TorrentInfo? {
        val contentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        var outputFilePath: String
        var fileName = ""
        if (!downloaded) {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    fileName = cursor.getString(0)
                }
            }
            if (fileName == "") throw Error("Source file name for creating torrent not found")
        } else {
            fileName = uri.toFile().name
        }
        val input =
            contentResolver.openInputStream(uri) ?: throw Resources.NotFoundException()
        outputFilePath = "${context.cacheDir}/$fileName"
        FileUtils.copyInputStreamToFile(input, File(outputFilePath))
        return createTorrent(outputFilePath)
    }

    /**
     * Creates a torrent from a file path string given as input
     * The extension of the file must be included (for example, .png)
     */
    fun createTorrent(filePath: String): TorrentInfo? {
        val file = File(filePath)
        if (!file.exists()) {
            //runOnUiThread { printToast("Something went wrong, check logs") }
            Log.i("litdao", "File doesn't exist!")
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

        val torrentName = filePath.substringBeforeLast('.') + ExtensionUtils.torrentDotExtension

        var os: OutputStream? = null
        try {
            os = FileOutputStream(
                File(
                    activity?.applicationContext?.cacheDir,
                    torrentName.split("/").last()
                ), true
            )
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
        try {
            val ti = TorrentInfo.bdecode(Vectors.byte_vector2bytes(buffer))
            val magnetLink =
                MagnetUtils.preHashString + ti.infoHash() + MagnetUtils.displayNameAppender + ti.name()
            Log.i("litdao", magnetLink)
            return ti
        } catch (e: Exception) {
            print(e)
        }
        return null
    }


    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 101) {
            val fileUri = data?.data

            if (fileUri != null) {

                val d =
                    activity?.let { DocumentFile.fromSingleUri(it.applicationContext, fileUri) }

                //TODO  Copy to cache
                if (d != null) {
                    selectedFile = d

                    view?.findViewById<TextView>(R.id.selected_literature)?.text =
                        "Selected FIle: " + d.name
                    //selected_literature

                }
            }
        } else if (requestCode == 100) {
            val fileUri = data?.data

            if (fileUri != null) {

                val d =
                    activity?.let { DocumentFile.fromSingleUri(it.applicationContext, fileUri) }


                //TODO  Copy to cache
                if (d != null) {
                    selectedFile = d

                    // Create Magnet URL
                    //val newTorrent = createTorrent(d.uri.path.toString())


                    // TODO load data from file
                    // custom title

                    // keyword extraction
                    //importFromInternalStorage(d)

                    // TODO add `title` to gossip

                    //selected_literature

                    // TODO: create Torrent
                    /*
                    if (newTorrent != null) {
                        literatureGossiper?.addTorrentInfo(newTorrent)
                    }
                    */

                    // TODO: Serialize Object
                    /*
                    Start Intent to Open the file..

                    var intent = Intent(Intent.ACTION_VIEW)
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.setDataAndType(d.uri, "application/pdf")
                    intent = Intent.createChooser(intent, "Open File")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                     */
                }
            }
        }
        @Suppress("DEPRECATION") // TODO: Fix deprecation issue.
        super.onActivityResult(requestCode, resultCode, data)
    }

}
