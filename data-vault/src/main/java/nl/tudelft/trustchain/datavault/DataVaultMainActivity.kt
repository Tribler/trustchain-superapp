package nl.tudelft.trustchain.datavault

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.datavault.ui.VaultBrowserFragment
import java.io.File
import java.util.*

class DataVaultMainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_datavault
    private val logTag = "Data Vault"

    private val currentFolder = MutableLiveData<File>()
    private val browserNavigationStack = Stack<File>()

    val VAULT by lazy { File(filesDir, VaultBrowserFragment.VAULT_DIR) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initVault()
        currentFolder.value = VAULT
    }

    private fun initVault() {
        if (!VAULT.exists()) {
            Log.e(logTag, "Data Vault not yet initiated. Initiating now.")
            VAULT.mkdir()
        }
    }

    fun setCurrentFolder(folder: File) {
        currentFolder.value = folder
    }

    fun getCurrentFolder(): LiveData<File> {
        return currentFolder
    }

    fun pushFolderToStack(folder: File) {
        browserNavigationStack.push(folder)
    }

    override fun onBackPressed() {
        if (browserNavigationStack.isEmpty()) {
            super.onBackPressed()
        } else {
            currentFolder.value = browserNavigationStack.pop()
        }
    }
}

