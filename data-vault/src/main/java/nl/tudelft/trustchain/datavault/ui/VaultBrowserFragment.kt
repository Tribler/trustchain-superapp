package nl.tudelft.trustchain.datavault.ui

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
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
import nl.tudelft.trustchain.datavault.accesscontrol.AccessPolicy
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import nl.tudelft.trustchain.datavault.community.DataVaultCommunity
import nl.tudelft.trustchain.datavault.databinding.VaultBrowserFragmentBinding
import nl.tudelft.trustchain.peerchat.ui.conversation.ConversationFragment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class VaultBrowserFragment : BaseFragment(R.layout.vault_browser_fragment) {
    private val binding by viewBinding(VaultBrowserFragmentBinding::bind)
    private lateinit var parentActivity: DataVaultMainActivity
    private lateinit var attestationCommunity: AttestationCommunity

    private val logTag = "DATA VAULT"
    private val adapter = ItemAdapter()

    private val VAULT by lazy { File(requireContext().filesDir, VAULT_DIR) }

    private var areFABsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentActivity = requireActivity() as DataVaultMainActivity

        attestationCommunity = IPv8Android.getInstance().getOverlay()!!
        attestationCommunity.trustedAuthorityManager.addTrustedAuthority(IPv8Android.getInstance().myPeer.publicKey)
        Log.e(logTag, "my peer: ${attestationCommunity.myPeer.publicKey.keyToBin().toHex()}")

        getDataVaultCommunity().setDataVaultActivity(parentActivity)
        getDataVaultCommunity().setVaultBrowserFragment(this)

        initVault()

        adapter.registerRenderer(VaultFileItemRenderer {
            val args = Bundle()
            args.putString(FILENAME, it.absolutePath)
            // args.putString(ConversationFragment.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
            findNavController().navigate(R.id.action_vaultBrowserFragment_to_accessManagementFragment, args)

            /*Log.e(logTag, "${it.absolutePath} exists: ${it.exists()}")
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.vault_file_fragment, null)
            val textView = view.findViewById<TextView>(R.id.text)
            textView.text = it.readText()
            val checkBox: CheckBox = view.findViewById(R.id.public_file_checkbox)

            val accessPolicy = AccessPolicy(it, attestationCommunity)
            checkBox.isChecked = accessPolicy.isPublic()

            checkBox.setOnCheckedChangeListener { _, value ->
                accessPolicy.setPublic(value)
            }

            builder.setTitle(it.name).setView(view)

            val dialog: AlertDialog = builder.create()
            dialog.show()*/
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        builder.setPositiveButton("Ok",
            DialogInterface.OnClickListener { _, _ ->
                // User clicked OK button
            }).
        setNegativeButton("Cancel",
            DialogInterface.OnClickListener { _, _ ->
                // User cancelled the dialog
            }).
        setItems(peers.map { peer ->  peer.mid}.toTypedArray(), DialogInterface.OnClickListener{ _, index ->
            val peer = peers[index]
            Log.e(logTag, "Chosen peer: ${peer.publicKey.keyToBin().toHex()}")

            // Currently all attestations. There must come a way to choose your attestations
            val attestations = attestationCommunity.database.getAllAttestations().filter { attestationBlob ->  attestationBlob.signature != null}


            /*val entries = attestationCommunity.database.getAllAttestations()
            .mapIndexed { index, blob -> DatabaseItem(index, blob) }.sortedBy {
                if (it.attestationBlob.metadata != null) {
                    return@sortedBy JSONObject(it.attestationBlob.metadata!!).optString("attribute")
                } else {
                    return@sortedBy ""
                }
            }*/

            getDataVaultCommunity().sendAccessibleFilesRequest(peer, Policy.READ, attestations)
        })

        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }

    private fun initVault() {
        if (!VAULT.exists()) {
            Log.e(logTag, "Data Vault not yet iniated. Initiating now.")
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

                    withContext(Dispatchers.Main) {
                        adapter.updateItems(vaultFileItems)
                    }

                    if (list.isNotEmpty()) {
                        Log.e("$logTag files", list.asList().reduce { acc, s ->  "$acc $s"}?: "empty")
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
        const val FILENAME = "file_name"
    }

    fun selectRequestableFile(peer: Peer, accessToken: String?, files: List<String>) {
        Log.e(logTag, "Selecting requestable file")
        requireActivity().runOnUiThread {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Select file to request").
            setPositiveButton("Ok",
                DialogInterface.OnClickListener { _, _ ->
                    // User clicked OK button
                }).
            setNegativeButton("Cancel",
                DialogInterface.OnClickListener { _, _ ->
                    // User cancelled the dialog
                }).
            setItems(files.toTypedArray(), DialogInterface.OnClickListener{ _, index ->
                Log.e(logTag, "item $index chosen")
                val file = files[index]
                getDataVaultCommunity().sendFileRequest(peer, Policy.READ, file, accessToken)
            })

            // Create the AlertDialog
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
    }

    fun notify(id: String, message: String) {
        requireActivity().runOnUiThread {
            Log.e(logTag, "File: $id")

            val builder = AlertDialog.Builder(context)
            builder.setTitle(id).setMessage(message).setPositiveButton("Ok", DialogInterface.OnClickListener { _, _ ->
                // close dialog
            })

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
    }

    private fun addTestFile() {
        val timestamp = Date().time
        val sdf = SimpleDateFormat("MM-dd-yyyy--HH:mm:ss", Locale.getDefault())
        val filename: String = sdf.format(timestamp)

        val fileContents = "Test file created on $filename"
        val fos = FileOutputStream (File(VAULT, filename))
        fos.write(fileContents.toByteArray())
        fos.close()

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
}
