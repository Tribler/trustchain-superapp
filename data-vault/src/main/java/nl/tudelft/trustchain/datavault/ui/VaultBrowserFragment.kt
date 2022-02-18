package nl.tudelft.trustchain.datavault.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

class VaultBrowserFragment : BaseFragment(R.layout.vault_browser_fragment) {
    private val binding by viewBinding(VaultBrowserFragmentBinding::bind)
    private lateinit var parentActivity: DataVaultMainActivity
    private lateinit var attestationCommunity: AttestationCommunity
    val acmViewModel: ACMViewModel by activityViewModels()

    private val logTag = "DATA VAULT"
    //private val adapter = ItemAdapter()
    private lateinit var adapter: PhotoGridAdapter
    private val uriPathHelper = URIPathHelper()

    private val VAULT by lazy { File(requireContext().filesDir, VAULT_DIR) }

    private var areFABsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentActivity = requireActivity() as DataVaultMainActivity

        attestationCommunity = IPv8Android.getInstance().getOverlay()!!
        attestationCommunity.trustedAuthorityManager.addTrustedAuthority(IPv8Android.getInstance().myPeer.publicKey)

        getDataVaultCommunity().setDataVaultActivity(parentActivity)
        getDataVaultCommunity().setVaultBrowserFragment(this)

        initVault()

        /*adapter.registerRenderer(VaultFileItemRenderer {
            acmViewModel.clearModifiedPolicies()
            val args = Bundle()
            args.putString(FILENAME, it.absolutePath)
            findNavController().navigate(R.id.action_vaultBrowserFragment_to_accessControlManagementFragment, args)

        val action = VaultBrowserFragmentDirections.actionVaultBrowserFragmentToAccessControlManagementFragment(it.absolutePath)
            findNavController().navigate(action)
        })*/
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PhotoGridAdapter(requireContext(), this, listOf<VaultFileItem>())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        setFABs()

        updateAdapter()
    }

    private fun setFABs() {
        binding.requestAccessibleFilesFab.visibility = View.GONE
        binding.requestAccessibleFilesText.visibility = View.GONE

        binding.deleteFilesFab.visibility = View.GONE
        binding.deleteFilesText.visibility = View.GONE

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

        binding.addFileFab.setOnClickListener {
            hideFabs()
            addTestFile()
        }
    }

    private fun showFabs() {
        binding.requestAccessibleFilesFab.show()
        binding.deleteFilesFab.show()
        binding.addFileFab.show()

        binding.requestAccessibleFilesText.visibility = View.VISIBLE
        binding.deleteFilesText.visibility = View.VISIBLE
        binding.addFileText.visibility = View.VISIBLE
        areFABsVisible = true
    }

    private fun hideFabs() {
        binding.requestAccessibleFilesFab.hide()
        binding.deleteFilesFab.hide()
        binding.addFileFab.hide()

        binding.requestAccessibleFilesText.visibility = View.GONE
        binding.deleteFilesText.visibility = View.GONE
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

            getDataVaultCommunity().sendAccessibleFilesRequest(peer, Policy.READ, attestations)
        }

        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }

    private fun initVault() {
        if (!VAULT.exists()) {
            Log.e(logTag, "Data Vault not yet initiated. Initiating now.")
            VAULT.mkdir()
        }
    }

    private fun updateAdapter() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (!VAULT.canRead()) {
                //setTitle(getTitle().toString() + " (inaccessible)")
                Log.e(logTag, "$VAULT inaccessible")
            } else {
                val list = VAULT.list()
                if (list != null) {
                    val vaultFileItems = list.asList().
                    filter { fileName -> !fileName.startsWith(".") && !fileName.endsWith(".acl")}.
                    map { fileName: String ->
                        VaultFileItem(File(VAULT, fileName), null)
                    }
                    Log.e(logTag, "Adapter updated. Vault files: $vaultFileItems")
                    withContext(Dispatchers.Main) {
                        adapter.updateItems(vaultFileItems)
                    }
                }
            }
        }
    }

    private fun getDataVaultCommunity(): DataVaultCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("DataVaultCommunity is not configured")
    }

    companion object {
        const val VAULT_DIR = "data_vault"
        const val FILENAME = "fileName"

        const val PICK_PHOTO = 100
        const val PERMISSION_REQUEST_CODE = 101
    }

    fun selectRequestableFile(peer: Peer, accessToken: String?, files: List<String>) {
        Log.e(logTag, "Selecting requestable file")
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
        }
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

    private fun addTestFile() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(),
            android.Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_MEDIA_LOCATION), PERMISSION_REQUEST_CODE)
        } else {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_PHOTO)
        }

        /*val timestamp = Date().time
        val sdf = SimpleDateFormat("MM-dd-yyyy--HH:mm:ss", Locale.getDefault())
        val filename: String = sdf.format(timestamp)

        val fileContents = "Test file created on $filename"
        val fos = FileOutputStream (File(VAULT, filename))
        fos.write(fileContents.toByteArray())
        fos.close()

        updateAdapter()*/
    }

    private fun addImageToVault(uri: Uri) {
        val filePath = uriPathHelper.getPath(requireContext(), uri)
        val file = File(filePath ?: "")
        Log.e(logTag, "File exists?: ${file.exists()}")

        //if (file.canRead()){
            val timestamp = Date().time
            val sdf = SimpleDateFormat("MM-dd-yyyy--HH:mm:ss", Locale.getDefault())
            val filename: String = sdf.format(timestamp)

            val fos = FileOutputStream (File(VAULT, filename))
            fos.write(file.readBytes())
            fos.close()

            Log.e(logTag, "Imaged added to vault")
       /* } else {
            Log.e(logTag, "Can not add image to vault")
        }*/

        updateAdapter()
    }

    private fun deleteTestFiles() {
        if (!VAULT.canRead()) {
            //
        } else {
            VAULT.list()?.forEach { fileName ->
                val testFile = File(VAULT, fileName)
                testFile.delete()
            }
        }

        updateAdapter()
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
}
