package nl.tudelft.trustchain.datavault.ui

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.TimingUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.datavault.DataVaultMainActivity
import nl.tudelft.trustchain.datavault.PerformanceTest
import nl.tudelft.trustchain.datavault.R
import nl.tudelft.trustchain.datavault.accesscontrol.AccessControlFile
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import nl.tudelft.trustchain.datavault.community.DataVaultCommunity
import nl.tudelft.trustchain.datavault.databinding.VaultBrowserFragmentBinding
import nl.tudelft.trustchain.datavault.tools.URIPathHelper
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
    private lateinit var uriPathHelper: URIPathHelper

    private var areFABsVisible = false

    val PERFORMANCE_TEST = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataVaultActivity = requireActivity() as DataVaultMainActivity

        attestationCommunity = IPv8Android.getInstance().getOverlay()!!
        attestationCommunity.trustedAuthorityManager.addTrustedAuthority(IPv8Android.getInstance().myPeer.publicKey)

        uriPathHelper = URIPathHelper(requireContext())

        getDataVaultCommunity().setDataVaultActivity(dataVaultActivity)
        getDataVaultCommunity().vaultBrowserFragment = this
        getDataVaultCommunity().setEVAOnReceiveCompleteCallback { peer, info, _, data ->
            when (info) {
                DataVaultCommunity.EVAId.EVA_DATA_VAULT_FILE -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        if (data != null) {
                            getDataVaultCommunity().onFilePacket(Packet(peer.address, data))
                        }
                    }
                }
                DataVaultCommunity.EVAId.TEST_EVA_DATA_VAULT_FILE -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        if (data != null) {
                            getDataVaultCommunity().onTestFilePacket(Packet(peer.address, data))
                        }
                    }
                }
            }
        }

        if (PERFORMANCE_TEST) {
            val testDir = File(
                getDataVaultCommunity().VAULT,
                "PERFORMANCE_TEST"
            ).apply {mkdirs()}

            testDir.listFiles()?.forEach {
                it.delete()
            }

            requireContext().assets.list("vault/PERFORMANCE_TEST")?.forEach {
//                Log.e("Files", it)
                val inputStream = requireContext().assets.open("vault/PERFORMANCE_TEST/$it")
                File(testDir, it).writeBytes(inputStream.readBytes())
            }

            val performanceTest = PerformanceTest(getDataVaultCommunity())

//            Be careful turning this on
//            performanceTest.measureEncDecryption(false)

            val attestationCount = attestationCommunity.database.getAllAttestations().size
            val rest = 3 - attestationCount
            if (rest > 0) {
                for (i in 1..rest) performanceTest.addTestAttestation(attestationCommunity)
            }


//            performanceTest.testTCID(attestationCommunity)

//            performanceTest.testDirectoryTree()

            val att = attestationCommunity.database.getAllAttestations().first()
            val rounds = 10
            var tot = 0L
            for (i in  0 until rounds) {
                val start = TimingUtils.getTimestamp()
                val filtered = AccessControlFile.filterAttestations(attestationCommunity.myPeer, listOf(att))
                val duration = TimingUtils.getTimestamp() - start
                Log.e(tag, "${duration} ms Attestation verified=${filtered?.size ?: 0 > 0}")
                tot += duration
            }
            Log.e(tag, "${tot / rounds} ms Average attestation verification time")

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BrowserGridAdapter(this, mutableListOf())
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter

        setFABs()

        dataVaultActivity.getCurrentFolder().observe(viewLifecycleOwner) { vaultFile ->
            Log.e(logTag, "Current folder changed: ${vaultFile.name}")
            when (vaultFile) {
                is LocalVaultFileItem -> updateAdapter(localVaultFiles())
                is PeerVaultFileItem -> updateAdapter(vaultFile.subFiles)
            }
        }
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
        if (currentFolder !is PeerVaultFileItem){
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

            if (PERFORMANCE_TEST) {
                PerformanceTest(getDataVaultCommunity()).testFileRequests(attTypeTest, peer, attestationCommunity)
                return@setItems
            }

            // Currently all attestations. There must come a way to choose your attestations
            val attestations = attestationCommunity.database.getAllAttestations()
                .filter { attestationBlob -> attestationBlob.signature != null }.map {
                    it.serialize().toHex()
                }

            val peerVaultFolder = PeerVaultFileItem(getDataVaultCommunity(), peer, null, VAULT_DIR, null)
            navigateToFolder(peerVaultFolder, peer.mid)

            getDataVaultCommunity().sendAccessibleFilesRequest(peerVaultFolder, VAULT_DIR, Policy.READ, Policy.AccessTokenType.TCID, attestations)
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

    private fun updateAdapter(vaultFileItems: List<VaultFileItem>?) {
        // Can be more efficient to add/remove single item and notify item inserted or removed
        CoroutineScope(Dispatchers.Main).launch {
            when {
                vaultFileItems.isNullOrEmpty() -> {
                    adapter.clearItems()
                }
                vaultFileItems.isNotEmpty() -> {
                    adapter.updateItems(vaultFileItems)
                }
            }
        }
    }

    fun updateAccessibleFiles(peerVaultFolder: PeerVaultFileItem, accessToken: String?, files: List<String>) {
        peerVaultFolder.updateSubFiles(accessToken, files)

        if (currentFolder == peerVaultFolder) {
            Log.e(logTag, "Refreshing browser view of ${peerVaultFolder.name} (${peerVaultFolder.subFiles?.size})")
            updateAdapter(peerVaultFolder.subFiles)
            // dataVaultActivity.setCurrentFolder(peerVaultFolder)
        } else {
            Log.e(logTag, "${peerVaultFolder.name} not current folder")
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            addImageToVault(uri)
        }
    }

    private fun addTestFile() {
//        pickImage.launch("image/*")
        if (currentFolder.name.lowercase().contains("vacation")) {
            requireContext().assets.list("vault/vacation")?.forEach {
                Log.e("Files", it)
                val inputStream = requireContext().assets.open("vault/vacation/$it")
                File(currentFolder.file, it).writeBytes(inputStream.readBytes())
            }
        } else if (currentFolder.name.lowercase().contains("architecture")) {
            requireContext().assets.list("vault/architecture")?.forEach {
                Log.e("Files", it)
                val inputStream = requireContext().assets.open("vault/architecture/$it")
                File(currentFolder.file, it).writeBytes(inputStream.readBytes())
            }
        } else {
            requireContext().assets.list("vault/root")?.forEach {
                Log.e("Files", it)
                val inputStream = requireContext().assets.open("vault/root/$it")
                File(currentFolder.file, it).writeBytes(inputStream.readBytes())
            }
        }

        updateAdapter(localVaultFiles())
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

    fun navigateToFolder(folder: VaultFileItem, title: String? = null) {
        dataVaultActivity.pushFolderToStack(currentFolder)
        dataVaultActivity.setCurrentFolder(folder)
        if (folder is PeerVaultFileItem) {
            dataVaultActivity.setActionBarTitle("Peer: ${title ?: folder.name}")
        } else {
            dataVaultActivity.setActionBarTitle(title ?: folder.name)
        }
    }

    private fun addImageToVault(uri: Uri) {
        Log.e("VBF", "Added photo with URI $uri, path: ${uri.path}")
        val filePath = uriPathHelper.getPath(uri)
        val file = File(filePath ?: "")

        if (!file.canRead()) {
            return
        }

        File(currentFolder.file, file.name).writeBytes(file.readBytes())

        Log.e(logTag, "Imaged added to vault")

        updateAdapter(localVaultFiles())
    }

    var attTypeTest = Policy.AccessTokenType.SESSION_TOKEN
    private fun deleteTestFiles() {
        if (PERFORMANCE_TEST) {
            attTypeTest = when (attTypeTest) {
                Policy.AccessTokenType.SESSION_TOKEN -> Policy.AccessTokenType.TCID
                Policy.AccessTokenType.TCID -> Policy.AccessTokenType.JWT
                Policy.AccessTokenType.JWT -> Policy.AccessTokenType.JSONLD
                Policy.AccessTokenType.JSONLD -> Policy.AccessTokenType.SESSION_TOKEN
            }

            Toast.makeText(requireContext(), "Chosen AccessTokenType: $attTypeTest", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentFolder.file.canRead()) {
            currentFolder.file.listFiles()?.forEach { it.delete() }
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

    private fun getDataVaultCommunity(): DataVaultCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("DataVaultCommunity is not configured")
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

        const val PERMISSION_REQUEST_CODE = 101
    }
}
