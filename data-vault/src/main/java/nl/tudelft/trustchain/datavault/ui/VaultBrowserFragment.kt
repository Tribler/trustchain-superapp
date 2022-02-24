package nl.tudelft.trustchain.datavault.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.datavault.DataVaultMainActivity
import nl.tudelft.trustchain.datavault.R
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import nl.tudelft.trustchain.datavault.community.DataVaultCommunity
import nl.tudelft.trustchain.datavault.databinding.VaultBrowserFragmentBinding
import java.io.ByteArrayOutputStream
import java.io.File

class VaultBrowserFragment : BaseFragment(R.layout.vault_browser_fragment) {
    private val binding by viewBinding(VaultBrowserFragmentBinding::bind)
    private val logTag = "DATA VAULT"

    private lateinit var dataVaultActivity: DataVaultMainActivity
    private lateinit var attestationCommunity: AttestationCommunity
    val acmViewModel: ACMViewModel by activityViewModels()

    private val currentFolder: VaultFileItem get() {
        return dataVaultActivity.getCurrentFolder().value!!
    }

    private lateinit var adapter: BrowserGridAdapter
    lateinit var uriPathHelper: URIPathHelper

    private var areFABsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataVaultActivity = requireActivity() as DataVaultMainActivity

        attestationCommunity = IPv8Android.getInstance().getOverlay()!!
        attestationCommunity.trustedAuthorityManager.addTrustedAuthority(IPv8Android.getInstance().myPeer.publicKey)

        uriPathHelper = URIPathHelper(requireContext())

        getDataVaultCommunity().setDataVaultActivity(dataVaultActivity)
        getDataVaultCommunity().setVaultBrowserFragment(this)
        getDataVaultCommunity().setEVAOnReceiveCompleteCallback { peer, info, id, data ->
            when (info) {
                DataVaultCommunity.EVAId.EVA_DATA_VAULT_FILE -> {
                    Log.e(logTag, "EVA Data vault file received from ${peer.mid}: $id")
                    CoroutineScope(Dispatchers.Main).launch {
                        if (data != null) {
                            getDataVaultCommunity().onFile(peer, id, data)
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BrowserGridAdapter(this, listOf())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = VaultBrowserLayoutManager(requireContext(), 2)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        setFABs()

        /*dataVaultActivity.getCurrentLocalFolder().observe(viewLifecycleOwner, {
                _ -> updateAdapter(localVaultFiles())
        })

        dataVaultActivity.getCurrentPeerFolder().observe(viewLifecycleOwner, {
                vaultFile -> updateAdapter(vaultFile.children!!)
        })*/

        dataVaultActivity.getCurrentFolder().observe(viewLifecycleOwner, {
                vaultFile -> when(vaultFile) {
                    is LocalVaultFileItem -> updateAdapter(localVaultFiles())
                    is PeerVaultFileItem -> updateAdapter(vaultFile.children!!)
                }
        })
    }

    private fun setFABs() {
        binding.requestAccessibleFilesFab.visibility = View.GONE
        binding.requestAccessibleFilesText.visibility = View.GONE

        binding.deleteFilesFab.visibility = View.GONE
        binding.deleteFilesText.visibility = View.GONE

        binding.createFolderFab.visibility = View.GONE
        binding.createFolderText.visibility = View.GONE

        binding.addFileFab.visibility = View.GONE
        binding.addFileText.visibility = View.GONE

        binding.actionFab.setOnClickListener {
            if (!areFABsVisible) {
                showFabs()
            } else {
                hideFabs()
            }
        }

        binding.requestAccessibleFilesFab.setOnClickListener {
            hideFabs()
            selectPeerDialog()
        }

        binding.deleteFilesFab.setOnClickListener {
            hideFabs()
            deleteTestFiles()
        }

        binding.createFolderFab.setOnClickListener {
            hideFabs()
            createFolder()
        }

        binding.addFileFab.setOnClickListener {
            hideFabs()
            addTestFile()
        }
    }

    private fun showFabs() {
        if (dataVaultActivity.getCurrentFolder().value !is PeerVaultFileItem){
            binding.requestAccessibleFilesFab.show()
            binding.deleteFilesFab.show()
            binding.createFolderFab.show()
            binding.addFileFab.show()

            binding.requestAccessibleFilesText.visibility = View.VISIBLE
            binding.deleteFilesText.visibility = View.VISIBLE
            binding.createFolderText.visibility = View.VISIBLE
            binding.addFileText.visibility = View.VISIBLE

            areFABsVisible = true
        }
    }

    private fun hideFabs() {
        binding.requestAccessibleFilesFab.hide()
        binding.deleteFilesFab.hide()
        binding.createFolderFab.hide()
        binding.addFileFab.hide()

        binding.requestAccessibleFilesText.visibility = View.GONE
        binding.deleteFilesText.visibility = View.GONE
        binding.createFolderText.visibility = View.GONE
        binding.addFileText.visibility = View.GONE
        areFABsVisible = false
    }

    private fun selectPeerDialog() {
        val peers = getDataVaultCommunity().getPeers()
        peers.forEach {
            Log.e(logTag, "Peer: ${it.publicKey.keyToBin().toHex()}")
        }

        val builder = AlertDialog.Builder(context)

        if (peers.isEmpty()) {
            builder.setTitle("No peers found")
        } else {
            builder.setTitle("Select peer")
        }

        builder.setPositiveButton("Ok") { _, _ ->
            // User clicked OK button
        }.
        setNegativeButton("Cancel") { _, _ ->
            // User cancelled the dialog
        }.
        setItems(peers.map { peer ->  peer.mid}.toTypedArray()) { _, index ->
            val peer = peers[index]
            Log.e(logTag, "Chosen peer: ${peer.publicKey.keyToBin().toHex()}")

            // Currently all attestations. There must come a way to choose your attestations
            val attestations = attestationCommunity.database.getAllAttestations()
                .filter { attestationBlob -> attestationBlob.signature != null }

            getDataVaultCommunity().sendAccessibleFilesRequest(peer, Policy.READ, attestations, null)
            adapter.requested = 0
        }

        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }

    private fun localVaultFiles(): List<VaultFileItem> {
        if (currentFolder is LocalVaultFileItem) {
            if (!currentFolder.file.canRead()) {
                Log.e(logTag, "$currentFolder inaccessible")
            } else {
                val list = currentFolder.file.list()
                if (list != null) {
                    return list.asList()
                        .filter { fileName -> !fileName.startsWith(".") && !fileName.endsWith(".acl") }
                        .map { fileName: String ->
                            LocalVaultFileItem(
                                requireContext(),
                                File(currentFolder.file, fileName),
                                null
                            )
                        }
                }
            }
        }

        return listOf()
    }

    private fun updateAdapter(vaultFileItems: List<VaultFileItem>) {
        // Can be more efficient to add/remove single item and notify item inserted or removed
        adapter.updateItems(vaultFileItems)
    }

    fun browseRequestableFiles(peer: Peer, accessToken: String?, files: List<String>) {
        files.forEach {
            Log.e(logTag, "peer files: $it")
        }


        val peerVaultFiles = files.map { fileName ->
            PeerVaultFileItem(getDataVaultCommunity(), peer, accessToken, fileName, null)
         }

        val peerCurrentFolder = PeerVaultFileItem(getDataVaultCommunity(), peer, accessToken, VAULT_DIR, peerVaultFiles)

        CoroutineScope(Dispatchers.Main).launch {
            dataVaultActivity.setCurrentFolder(peerCurrentFolder)
        }

        //updateAdapter(peerVaultFiles)


    /*Log.e(logTag, "Selecting requestable file")
        requireActivity().runOnUiThread {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Select file to request").
            setPositiveButton("Ok") { _, _ ->
                // User clicked OK button
            }.
            setNegativeButton("Cancel") { _, _ ->
                // User cancelled the dialog
            }.
            setItems(files.toTypedArray()) { _, index ->
                Log.e(logTag, "item $index chosen")
                val file = files[index]
                getDataVaultCommunity().sendFileRequest(peer, Policy.READ, file, accessToken)
            }

            // Create the AlertDialog
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }*/
    }

    private fun addTestFile() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.e(logTag, "Requesting permission to pick media")
                // Request permissions
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        } else {
            Log.e(logTag, "Permission to pick media")
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_PHOTO)
        }
    }

    private fun createFolder() {
        val builder = AlertDialog.Builder(context)

        val view = layoutInflater.inflate(R.layout.create_folder_dialog, null)
        builder.setView(view)

        val folderNameEditText = view.findViewById<EditText>(R.id.folderName)

        builder.setTitle("Create folder").setNegativeButton("Cancel") { _, _ -> }
            .setPositiveButton("Ok") { _, _ ->
            Log.e(logTag, "folder name: ${folderNameEditText.text}")
            CoroutineScope(Dispatchers.IO).launch {
                val newFolder = File(currentFolder.file, folderNameEditText.text.toString())
                newFolder.mkdir()
                withContext(Dispatchers.Main) {
                    updateAdapter(localVaultFiles())
                }
            }
        }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }

    fun navigateToFolder(folder: VaultFileItem) {
        dataVaultActivity.pushFolderToStack(currentFolder)
        dataVaultActivity.setCurrentFolder(folder)
        dataVaultActivity.setActionBarTitle("${folder.name}")
    }

    private fun addImageToVault(uri: Uri) {
        val filePath = uriPathHelper.getPath(uri)
        val file = File(filePath ?: "")

        val options = BitmapFactory.Options().also {
            it.outWidth = VaultFileItem.IMAGE_WIDTH
            it.outHeight = VaultFileItem.IMAGE_WIDTH
        }
        val bitmap = BitmapFactory.decodeFile(filePath, options)
        val os = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, os)

        /*if (!file.canRead()) {
            return
        }*/

        File(currentFolder.file, file.name).writeBytes(os.toByteArray())

        Log.e(logTag, "Imaged added to vault")

        updateAdapter(localVaultFiles())
    }

    private fun deleteTestFiles() {
        if (!currentFolder.file.canRead()) {
            //
        } else {
            currentFolder.file.list()?.forEach { fileName ->
                val testFile = File(currentFolder.file, fileName)
                testFile.delete()
            }
        }

        updateAdapter(localVaultFiles())
    }

    fun notify(id: String, message: String) {
        requireActivity().runOnUiThread {
            Log.e(logTag, "File: $id")

            val builder = AlertDialog.Builder(context)
            builder.setTitle(id).setMessage(message).setPositiveButton("Ok") { _, _ ->
                // close dialog
            }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
    }

    fun getDataVaultCommunity(): DataVaultCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("DataVaultCommunity is not configured")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_PHOTO){
            val uri = data?.data
            if (uri != null) {
                addImageToVault(uri)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.

                if (grantResults.isNotEmpty() &&
                        grantResults.all { result -> result == PackageManager.PERMISSION_GRANTED}) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    addTestFile()
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    notify("Permission denied", "Permissions required to access photo library was not granted.")
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    companion object {
        const val VAULT_DIR = "data_vault"
        const val FILENAME = "fileName"

        const val PICK_PHOTO = 100
        const val PERMISSION_REQUEST_CODE = 101
    }
}
