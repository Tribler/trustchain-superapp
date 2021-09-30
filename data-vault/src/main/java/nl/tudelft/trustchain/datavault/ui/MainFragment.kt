package nl.tudelft.trustchain.datavault.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.vault_file_item.*
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.datavault.DataVaultMainActivity
import nl.tudelft.trustchain.datavault.R
import nl.tudelft.trustchain.datavault.databinding.MainFragmentBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class MainFragment : BaseFragment(R.layout.main_fragment) {
    private val binding by viewBinding(MainFragmentBinding::bind)
    private lateinit var parentActivity: DataVaultMainActivity

    private val logTag = "DATA VAULT"
    private val adapter = ItemAdapter()

    private val VAULT by lazy { File(requireContext().filesDir,"data_vault") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentActivity = requireActivity() as DataVaultMainActivity

        initVault()

        writeTestFiles()
        //deleteTestFiles()

        adapter.registerRenderer(VaultFileItemRenderer())

        /*val NUM_BYTES_NEEDED_FOR_MY_APP = 1024 * 1024 * 10L;

        val storageManager = requireContext().applicationContext.getSystemService<StorageManager>()!!
        val appSpecificInternalDirUuid: UUID = storageManager.getUuidForPath(dataVaultDir)
        val availableBytes: Long =
            storageManager.getAllocatableBytes(appSpecificInternalDirUuid)
        if (availableBytes >= NUM_BYTES_NEEDED_FOR_MY_APP) {
            storageManager.allocateBytes(
                appSpecificInternalDirUuid, NUM_BYTES_NEEDED_FOR_MY_APP)
        } else {
            val storageIntent = Intent().apply {
                // To request that the user remove all app cache files instead, set
                // "action" to ACTION_CLEAR_APP_CACHE.
                action = ACTION_MANAGE_STORAGE
            }
        }*/

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

        //Log.e(logTag, "is directory: ${vaultFile.isDirectory}")
        Log.e(logTag, "free space: ${VAULT.freeSpace}")
        if (!VAULT.canRead()) {
            //setTitle(getTitle().toString() + " (inaccessible)")
            Log.e(logTag, "$VAULT inaccessible")
            return
        }


        val list = VAULT.list()
        Log.e(logTag + " files", list?.asList()?.reduce { acc, s ->  acc + " $s"}?: "empty")
        if (list != null) {
            val vaultFileItems = list.asList().
                filter { fileName -> !fileName.startsWith(".") }.
                map { fileName: String ->
                VaultFileItem(File(fileName, ""), null)
            }
            adapter.updateItems(vaultFileItems)
        }
    }

    private fun vaultFile(filename: String): String {
        return "data_vault/$filename"
    }

    private fun writeTestFiles() {
        var filename = "testfile1"
        var fileContents = "Hello world once!"
        var fos = FileOutputStream (File(VAULT, filename))
        fos.write(fileContents.toByteArray())
        fos.close()


        filename = "testfile2"
        fileContents = "Hello world twice!"
        fos = FileOutputStream (File(VAULT, filename))
        fos.write(fileContents.toByteArray())
        fos.close()
    }

    private fun deleteTestFiles() {
        var testFile = File(VAULT,"testfile1")
        testFile.delete()
        testFile = File(VAULT,"testfile2")
        testFile.delete()
    }
}
