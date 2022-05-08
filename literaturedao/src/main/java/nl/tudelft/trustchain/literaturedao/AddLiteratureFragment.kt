package nl.tudelft.trustchain.literaturedao

import LiteratureGossiper
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.Vectors
import com.frostwire.jlibtorrent.swig.*
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.*
import nl.tudelft.trustchain.literaturedao.controllers.KeywordExtractor
import nl.tudelft.trustchain.literaturedao.controllers.PdfController
import nl.tudelft.trustchain.literaturedao.utils.ExtensionUtils
import nl.tudelft.trustchain.literaturedao.utils.MagnetUtils
import org.apache.commons.io.FileUtils
import java.io.*


class AddLiteratureFragment : Fragment(R.layout.fragment_literature_add) {

    private lateinit var selectedFile: DocumentFile
    private val s = SessionManager()
    private var literatureGossiper: LiteratureGossiper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view : View =  inflater.inflate(R.layout.fragment_literature_add, container, false)
        val selectFileUpload: Button = view.findViewById(R.id.select_new_lirterature) as Button
        val submitFileUpload: Button = view.findViewById(R.id.submit_new_lirterature) as Button

        selectFileUpload.setOnClickListener {
            // do something
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, 101);
        }

        submitFileUpload.setOnClickListener  {
            try {
                //TODO: Start Loading animation and start thread
                activity?.runOnUiThread {
                    view.findViewById<LinearLayout>(R.id.add_literature_content).visibility = View.GONE;
                    view.findViewById<LinearLayout>(R.id.add_literature_loading).visibility = View.VISIBLE;
                }

                lifecycleScope.launch {
                    val pdf = requireContext().contentResolver.openInputStream(selectedFile.uri);
                    PDFBoxResourceLoader.init(activity?.baseContext);

                    val strippedString = PdfController().stripText(pdf!!)
                    val kws: MutableList<Pair<String, Double>>

                    // initilaizing freq's
                    val csv: InputStream = context?.getAssets()?.open("stemmed_freqs.csv")
                        ?: throw Exception("Stemmed_freqs.csv not found");

                    // Extract keywords
                    kws = KeywordExtractor().extract(strippedString, csv);

                    // Create Torrent
                    val magnet = createTorrentFromFileUri(requireContext(), selectedFile.uri);
                    if (magnet != null) {
                        literatureGossiper?.addTorrentInfo(magnet)
                    }

                    // TODO: Create Literature object
                    val literatureTitle = view.findViewById<EditText>(R.id.literature_title).text;
                    val newLiterature = LiteratureDaoActivity.Literature(literatureTitle.toString(),magnet?.makeMagnetUri().toString(),kws,true);

                    print(newLiterature);

                    // TODO: Store Result locally

                    // TODO: Gossip Result

                    // TODO: Move to Home Screen
                    withContext(Dispatchers.Main) {
                        view.findViewById<LinearLayout>(R.id.add_literature_loading).visibility = View.GONE;
                        view.findViewById<LinearLayout>(R.id.add_literature_done).visibility = View.VISIBLE;
                    }
                }
            } catch (ex: Exception ) {
                // TODO: Show error
                ex.printStackTrace();
            }
        }
        // Inflate the layout for this fragment
        return view;
    }

    /**
     * Creates a torrent from a file uri in the given context
     */
    fun createTorrentFromFileUri(context: Context, uri: Uri): TorrentInfo? {
        val contentResolver = context.contentResolver
        val projection = arrayOf<String>(MediaStore.MediaColumns.DISPLAY_NAME)
        var fileName = ""
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(0)
            }
        }

        if (fileName == "") throw Error("Source file name for creating torrent not found")
        val input =
            contentResolver.openInputStream(uri) ?: throw Resources.NotFoundException()
        val outputFilePath = "${context.cacheDir}/$fileName"

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
            return null;
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
            os = FileOutputStream(File(activity?.applicationContext?.cacheDir, torrentName.split("/").last()),true)
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
            val magnetLink = MagnetUtils.preHashString + ti.infoHash() + MagnetUtils.displayNameAppender + ti.name()

            Log.i("litdao", magnetLink)

            return ti
        } catch(e : Exception ) {
            print(e);


        }
        return null;
    }



    override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {
        if (requestCode == 101) {
            var fileUri = data?.data;

            if (fileUri != null) {

                val d =
                    activity?.let { DocumentFile.fromSingleUri(it.applicationContext, fileUri) }

                //TODO  Copy to cache
                if (d != null) {
                    selectedFile = d;

                    view?.findViewById<TextView>(R.id.selected_literature)?.text = "Selected FIle: " + d.name ;
                    //selected_literature

                }
            }
        } else if (requestCode == 100) {
            var fileUri = data?.data;

            if (fileUri != null) {

                val d =
                    activity?.let { DocumentFile.fromSingleUri(it.applicationContext, fileUri) }


                //TODO  Copy to cache
                if (d != null) {
                    selectedFile = d;

                    // Create Magnet URL;
                    //val newTorrent = createTorrent(d.uri.path.toString())


                    // TODO load data from file
                    // custom title

                    // keyword extraction
                    //importFromInternalStorage(d)

                    // TODO add to gossip
                    val title: String = "sadasasd";

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

                    var intent = Intent(Intent.ACTION_VIEW);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(d.uri, "application/pdf");
                    intent = Intent.createChooser(intent, "Open File");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                     */
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
