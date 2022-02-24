package nl.tudelft.trustchain.datavault

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.datavault.ui.LocalVaultFileItem
import nl.tudelft.trustchain.datavault.ui.VaultBrowserFragment
import nl.tudelft.trustchain.datavault.ui.VaultFileItem
import java.io.File
import java.util.*

class DataVaultMainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_datavault
    private val logTag = "Data Vault"

    private val currentFolder = MutableLiveData<VaultFileItem>()
    private val browserNavigationStack = Stack<VaultFileItem>()

    val VAULT by lazy { File(filesDir, VaultBrowserFragment.VAULT_DIR) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initVault()
        currentFolder.value = LocalVaultFileItem(this, VAULT, null)
    }

    private fun initVault() {
        if (!VAULT.exists()) {
            Log.e(logTag, "Data Vault not yet initiated. Initiating now.")
            VAULT.mkdir()
        }
    }

    fun setCurrentFolder(folder: VaultFileItem) {
        currentFolder.value = folder
    }

    fun getCurrentFolder(): LiveData<VaultFileItem> {
        return currentFolder
    }

    fun pushFolderToStack(folder: VaultFileItem) {
        browserNavigationStack.push(folder)
    }

    fun setActionBarTitle(title: String?) {
        supportActionBar?.subtitle = title
    }

    override fun onBackPressed() {
        // Check when browsing else regular onBackPressed
        val currentFragmentId = supportFragmentManager.primaryNavigationFragment?.findNavController()?.currentDestination?.id
        if (currentFragmentId == R.id.vaultBrowserFragment) {
            if (browserNavigationStack.isEmpty()) {
                super.onBackPressed()
            } else if (browserNavigationStack.size == 1) {
                // Local vault home
                currentFolder.value = browserNavigationStack.pop()
                setActionBarTitle(null)
            } else {
                currentFolder.value = browserNavigationStack.pop()
                setActionBarTitle("${currentFolder.value!!.name}")
            }
        } else {
            super.onBackPressed()
        }
    }
}

