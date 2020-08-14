package com.example.musicdao.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.musicdao.MusicService
import com.example.musicdao.R
import com.example.musicdao.ReleaseOverviewFragment
import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.TorrentInfo

/**
 * A form within a dialog which allows the user to submit a Release and publish it, by either
 * selecting local audio files or by pasting a magnet link
 */
class SubmitReleaseDialog(private val musicService: ReleaseOverviewFragment) : DialogFragment() {
    private var dialogView: View? = null
    private var localTorrentInfo: TorrentInfo? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        // Get the layout inflater
        val inflater = requireActivity().layoutInflater

        dialogView = inflater.inflate(R.layout.dialog_submit_release, null)

        dialogView?.findViewById<Button>(R.id.select_local)?.setOnClickListener {
            selectLocalTrackFile()
        }

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(dialogView)
            // Add action buttons
            .setPositiveButton("Submit",
                DialogInterface.OnClickListener { _, _ ->
                    val titleEditText = dialog?.findViewById<EditText>(R.id.title)
                    val artistEditText = dialog?.findViewById<EditText>(R.id.artists)
                    val releaseDateEditText =
                        dialog?.findViewById<EditText>(R.id.release_date)
                    val magnetEditText = dialog?.findViewById<EditText>(R.id.release_magnet)

                    if (titleEditText?.text == null || artistEditText?.text == null || releaseDateEditText?.text == null || magnetEditText?.text == null ||
                        titleEditText.text.isEmpty() || artistEditText.text.isEmpty() || releaseDateEditText.text.isEmpty() || magnetEditText.text.isEmpty()
                    ) {
                        Toast.makeText(context, "Form is not complete", Toast.LENGTH_SHORT).show()
                    } else {
                        val torrentInfo = localTorrentInfo
                        val torrentInfoName = if (torrentInfo == null) {
                            // If we only have a magnet link, extract the name from it to use for the
                            // .torrent
                            val magnetLink = magnetEditText.text.toString()
                            Util.extractNameFromMagnet(magnetLink)
                        } else {
                            torrentInfo.name()
                        }

                        musicService.finishPublishing(
                            titleEditText.text,
                            artistEditText.text,
                            releaseDateEditText.text,
                            magnetEditText.text,
                            torrentInfoName
                        )
                    }
                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { _, _ ->
                    dialog?.cancel()
                })
        return builder.create()
    }

    /**
     * This is called when the chooseFile is completed
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // This should be reached when the chooseFile intent is completed and the user selected
        // an audio file
        val uriList = mutableListOf<Uri>()
        val singleFileUri = data?.data
        if (singleFileUri != null) {
            // Only one file is selected
            uriList.add(singleFileUri)
        }
        val clipData = data?.clipData
        if (clipData != null) {
            // Multiple files are selected
            val count = clipData.itemCount
            for (i in 0 until count) {
                val uri = clipData.getItemAt(i).uri
                uriList.add(uri)
            }
        }
        if (uriList.size < 1) return
        val localContext = context
        if (localContext != null) {
            val torrentFile = (activity as MusicService).generateTorrent(localContext, uriList)
            val torrentInfo = TorrentInfo(torrentFile)
            localTorrentInfo = torrentInfo
            dialogView?.findViewById<EditText>(R.id.release_magnet)
                ?.setText(torrentInfo.makeMagnetUri(), TextView.BufferType.EDITABLE)
        }
    }

    /**
     * Select an audio file from local disk
     */
    private fun selectLocalTrackFile() {
        val selectFilesIntent = Intent(Intent.ACTION_GET_CONTENT)
        selectFilesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        selectFilesIntent.type = "audio/*"
        val chooseFileActivity = Intent.createChooser(selectFilesIntent, "Choose a file")
        startActivityForResult(chooseFileActivity, 1)
    }
}
