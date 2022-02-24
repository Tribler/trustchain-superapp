package nl.tudelft.trustchain.datavault.ui

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import nl.tudelft.trustchain.datavault.community.DataVaultCommunity
import java.io.File

class PeerVaultFileItem(
    private val dataVaultCommunity: DataVaultCommunity,
    private val peer: Peer,
    private val accessToken: String?,
    val fileName: String,
    val children: List<PeerVaultFileItem>?
): VaultFileItem() {
    val cacheKey = "${peer.mid}_$fileName"

    override val file: File get() {
        return File(fileName)
    }

    fun getDataCacheFile(context: Context): File {
        return File(context.cacheDir, "$cacheKey.tmp")
    }

    fun writeDataCacheFile(context: Context, data: ByteArray): File {
        val cacheFile = File(context.cacheDir, "$cacheKey.tmp")
        cacheFile.writeBytes(data)
        return cacheFile
    }

     fun requestRemoteBitmap(viewHolder: ImageViewHolder) {
         Log.e("PeerVault", "Requesting data for $fileName")
         dataVaultCommunity.addPendingImageViewHolders(this, viewHolder)
         dataVaultCommunity.sendFileRequest(peer, Policy.READ, fileName, accessToken)
    }

    override val name: String get() {
        return file.name
    }

    override fun isDirectory(): Boolean {
        return fileName == "pics"
    }

    companion object {
        const val CACHE_FILE_EXPIRY_MILLIS: Long = 1 * 60 * 1000

    }
}
