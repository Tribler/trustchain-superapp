package nl.tudelft.trustchain.datavault.ui

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import nl.tudelft.trustchain.datavault.community.DataVaultCommunity
import java.io.File

class PeerVaultFileItem(
    private val dataVaultCommunity: DataVaultCommunity,
    val peer: Peer,
    private val accessToken: String?,
    private val _fileName: String,
    var subFiles: List<PeerVaultFileItem>?
): VaultFileItem() {

    val fileName: String get() {
        return if (_fileName.endsWith("/")) _fileName.dropLast(1) else _fileName
    }
    val cacheKey = getCacheKey(peer, fileName)

    override val file: File get() {
        return File(fileName)
    }

    fun getDataCacheFile(context: Context): File {
        return File(context.cacheDir, "$cacheKey.tmp")
    }

    fun writeDataCacheFile(context: Context, data: ByteArray): File {
        val cacheFile = File(context.cacheDir, "$cacheKey.tmp")
        // cacheFile.createNewFile()
        cacheFile.writeBytes(data)
        return cacheFile
    }

     fun requestRemoteBitmap(viewHolder: ImageViewHolder) {
         Log.e("PeerVault", "Requesting data for $fileName")
         dataVaultCommunity.addPendingImageViewHolders(this, viewHolder)
         dataVaultCommunity.sendFileRequest(peer, Policy.READ, fileName, accessToken)
    }

    fun fetchSubFolderAccessibleFiles() {
        Log.e("PeerVault", "Fetching sub files for $_fileName (${subFiles?.size ?: 0})")

        if (isDirectory()) {
            dataVaultCommunity.sendAccessibleFilesRequest(this, fileName, Policy.READ, accessToken, null)
        }
    }

    fun updateSubFiles(accessToken: String?, files: List<String>) {
        val peerVaultFiles = files.map { fileName ->
            PeerVaultFileItem(dataVaultCommunity, peer, accessToken, fileName, null)
        }

        subFiles = peerVaultFiles
    }

    override val name: String get() {
        return file.name
    }

    override fun isDirectory(): Boolean {
        Log.e("PeerVault", "is directory: ${_fileName.endsWith("/")}")
        return _fileName.endsWith("/")
    }

    companion object {
        const val CACHE_FILE_EXPIRY_MILLIS: Long = 10 * 60 * 1000 // 10 min expiry time

        fun getCacheKey(peer: Peer, id: String): String {
            return "${peer.mid}_${id.replace("/", "_")}"
        }

    }
}
