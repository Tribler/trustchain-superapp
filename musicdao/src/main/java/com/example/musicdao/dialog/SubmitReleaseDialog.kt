package com.example.musicdao.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.musicdao.MusicService
import com.example.musicdao.R
import com.example.musicdao.catalog.PlaylistsOverviewFragment
import com.example.musicdao.util.ReleaseFactory
import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.TorrentInfo

/**
 * A form within a dialog which allows the user to submit a Release and publish it, by either
 * selecting local audio files or by pasting a magnet link
 */
class SubmitReleaseDialog(private val playlistsOverviewFragment: PlaylistsOverviewFragment) :
    DialogFragment() {
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
            .setPositiveButton(
                "Submit",
                DialogInterface.OnClickListener { _, _ ->
                    submitRelease()
                }
            )
            .setNegativeButton(
                "Cancel",
                DialogInterface.OnClickListener { _, _ ->
                    dialog?.cancel()
                }
            )
        return builder.create()
    }

    /**
     * This is called when the chooseFile is completed
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        val uriList = ReleaseFactory.uriListFromLocalFiles(data)
        if (uriList.isEmpty()) return
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
     * Perform various validations of whether the input (meta)data is properly formatted. Then
     * create and submit a Release block or show an error to the user
     */
    private fun submitRelease() {
        val titleEditText = dialog?.findViewById<EditText>(R.id.title)
        val artistEditText = dialog?.findViewById<EditText>(R.id.artists)
        val releaseDateEditText =
            dialog?.findViewById<EditText>(R.id.release_date)
        val magnetEditText = dialog?.findViewById<EditText>(R.id.release_magnet)

        val torrentInfoName = validateReleaseBlock(
            titleEditText?.text.toString(),
            artistEditText?.text.toString(),
            releaseDateEditText?.text.toString(),
            magnetEditText?.text.toString()
        )
        if (torrentInfoName != null) {
            playlistsOverviewFragment.publish(
                magnetEditText?.text.toString(),
                titleEditText?.text.toString(),
                artistEditText?.text.toString(),
                releaseDateEditText?.text.toString(),
                torrentInfoName
            )
        } else {
            Toast.makeText(context, "Form is not complete", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Validate that a new Release block has the right metadata parameters and calculate the
     * torrentInfoName
     */
    fun validateReleaseBlock(
        title: String?,
        artist: String?,
        releaseDate: String?,
        magnet: String?
    ): String? {
        return if (title == null || artist == null || releaseDate == null || magnet == null ||
            title.isEmpty() || artist.isEmpty() || releaseDate.isEmpty() || magnet.isEmpty()
        ) {
            null
        } else {
            val torrentInfo = localTorrentInfo
            if (torrentInfo == null) {
                // If we only have a magnet link, extract the name from it to use for the
                // .torrent
                Util.extractNameFromMagnet(magnet)
            } else {
                torrentInfo.name()
            }
        }
    }

    /**
     * Select an audio file from local disk
     */
    private fun selectLocalTrackFile() {
        val selectFilesIntent = Intent(Intent.ACTION_GET_CONTENT)
        selectFilesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        selectFilesIntent.type = "audio/*"
        val chooseFileActivity = Intent.createChooser(selectFilesIntent, "Choose a file")
        startActivityForResult(chooseFileActivity, 1)
    }
}
