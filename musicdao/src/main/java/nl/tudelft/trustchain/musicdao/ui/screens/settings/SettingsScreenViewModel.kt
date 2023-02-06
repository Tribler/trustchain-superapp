package nl.tudelft.trustchain.musicdao.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import nl.tudelft.trustchain.musicdao.CachePath
import nl.tudelft.trustchain.musicdao.core.repositories.album.BatchPublisher
import nl.tudelft.trustchain.musicdao.ui.util.AndroidURIController
import dagger.hilt.android.lifecycle.HiltViewModel
import java.nio.file.Paths
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val batchPublisher: BatchPublisher,
    private val cachePath: CachePath,
    private val androidURIController: AndroidURIController
) : ViewModel() {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun publishBatch(uri: Uri, context: Context) {
        Log.d("MusicDao", "publishBatch: $uri")
        val path = Paths.get("${cachePath.getPath()}/batch_publish/output.csv")
        val output = androidURIController.copyIntoCache(uri, context, path) ?: return
        batchPublisher.publish(output)
    }
}
