package nl.tudelft.trustchain.datavault.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.vault_browser_fragment.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.datavault.DataVaultMainActivity
import nl.tudelft.trustchain.datavault.R
import nl.tudelft.trustchain.datavault.community.DataVaultCommunity
import nl.tudelft.trustchain.datavault.databinding.VaultBrowserFragmentBinding
import java.io.File
import java.io.FileOutputStream

class VaultBrowserFragment : BaseFragment(R.layout.vault_browser_fragment) {
    private val binding by viewBinding(VaultBrowserFragmentBinding::bind)
    private lateinit var parentActivity: DataVaultMainActivity
    private lateinit var attestationCommunity: AttestationCommunity

    private val logTag = "DATA VAULT"
    private val adapter = ItemAdapter()

    val VAULT by lazy { File(requireContext().filesDir, VAULT_DIR) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentActivity = requireActivity() as DataVaultMainActivity

        attestationCommunity = IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
        attestationCommunity.trustedAuthorityManager.addTrustedAuthority(IPv8Android.getInstance().myPeer.publicKey)

        getDataVaultCommunity().setDataVaultActivity(parentActivity)
        getDataVaultCommunity().setVaultBrowserFragment(this)

        initVault()

        //writeTestFiles()
        //deleteTestFiles()

        adapter.registerRenderer(VaultFileItemRenderer {
            Log.e(logTag, "${it.absolutePath} exists: ${it.exists()}")
            val builder: AlertDialog.Builder = activity.let {
                AlertDialog.Builder(requireContext())
            }

            builder.setMessage(it.readText())

            val dialog: AlertDialog = builder.create()
            dialog.show()
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

        requestButton.setOnClickListener {
            val publicKey = defaultCryptoProvider.keyFromPublicBin("4c69624e61434c504b3a77a4c00e7a40a611c7f079c5d7d7505ea2f59622c443bfa4e6bf9a18a8467a7d106d8a3691e061eb80cd43fbeed9d1f50217e7f619167768395ebac401a76795".hexToBytes())
            val peer = getDataVaultCommunity().getPeers().find { it.mid == publicKey.keyToHash().toHex() }
            if (peer == null){
                Toast.makeText(requireContext(), "Test peer not found", Toast.LENGTH_SHORT).show()
            } else {
                // Currently all attestations. There must come a way to choose your attestations
                val attestations = attestationCommunity.database.getAllAttestations()
                getDataVaultCommunity().sendFileRequest(peer, "testfile1", attestations)
            }
        }

        initAdapter()
    }

    override fun onResume() {
        super.onResume()
        parentActivity.setActionBarTitle("Data Vault")
    }

    private fun initVault() {
        if (!VAULT.exists()) {
            Log.e(logTag, "Data Vault not yet iniated. Initiating now.")
            VAULT.mkdir()
        }
    }

    private fun initAdapter() {
        Log.e(logTag, "vault dir: $VAULT")
        Log.e(logTag, "free space: ${VAULT.freeSpace}")
        //Log.e(logTag, "is directory: ${vaultFile.isDirectory}")
        if (!VAULT.canRead()) {
            //setTitle(getTitle().toString() + " (inaccessible)")
            Log.e(logTag, "$VAULT inaccessible")
            return
        }


        val list = VAULT.list()
        if (list != null) {
            val vaultFileItems = list.asList().
                filter { fileName -> !fileName.startsWith(".") }.
                map { fileName: String ->
                    VaultFileItem(File(VAULT, fileName), null)
            }
            adapter.updateItems(vaultFileItems)

            if (!list.isEmpty()) {
                Log.e(logTag + " files", list.asList().reduce { acc, s ->  acc + " $s"}?: "empty")
            }
        }
    }

    private fun getDataVaultCommunity(): DataVaultCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("DataVaultCommunity is not configured")
    }

    companion object {
        val VAULT_DIR = "data_vault"
    }

    fun notify(message: String) {
        val myPublicKey = getIpv8().myPeer.publicKey.keyToBin().toHex()
        requireActivity().runOnUiThread {
            Log.e(logTag, "NOTICE: $message")
            Log.e(logTag, myPublicKey)

            Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
            /*val builder: AlertDialog.Builder = requireContext().let {
                AlertDialog.Builder(requireContext())
            }
            builder.setMessage(message)

            val dialog: AlertDialog = builder.create()
            dialog.show()*/


            status.text = message
            adapter.notifyDataSetChanged()
        }
    }

    private fun writeTestFiles() {
        var filename = "testfile1"
        var fileContents = "Testing data vault"
        var fos = FileOutputStream (File(VAULT, filename))
        fos.write(fileContents.toByteArray())
        fos.close()


        /*filename = "testfile2"
        fileContents = "Hello world twice!"
        fos = FileOutputStream (File(VAULT, filename))
        fos.write(fileContents.toByteArray())
        fos.close()*/
    }

    private fun deleteTestFiles() {
        var testFile = File(VAULT,"testfile1")
        testFile.delete()
        testFile = File(VAULT,"testfile2")
        testFile.delete()
    }
}
