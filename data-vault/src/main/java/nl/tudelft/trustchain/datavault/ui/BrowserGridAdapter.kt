package nl.tudelft.trustchain.datavault.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.trustchain.datavault.R
import java.io.File
import java.util.*

class BrowserGridAdapter(
    private val vaultBrowserFragment: VaultBrowserFragment,
    private val files: MutableList<VaultFileItem>
    ): RecyclerView.Adapter<ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(LayoutInflater.from(vaultBrowserFragment.requireContext()).inflate(R.layout.browser_grid_item, parent, false))
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.setLoading(true)

        val vaultFile = files[position]

        holder.name.text = vaultFile.name
        holder.container.setOnClickListener(null)

        if (vaultFile.isDirectory()) {
            holder.name.visibility = View.VISIBLE
            holder.setImageResource(R.mipmap.folder_icon)

            holder.container.setOnClickListener {
                Log.e("Adapter", "Folder onClick")
                vaultBrowserFragment.navigateToFolder(vaultFile)
            }

            if (vaultFile is PeerVaultFileItem) vaultFile.fetchSubFolderAccessibleFiles()
        } else {
            when (vaultFile) {
                is LocalVaultFileItem -> {
                    val bitmap = vaultFile.getImage(holder.inBitmap)!!
                    holder.setImageBitmap(bitmap)

                    holder.container.setOnClickListener {
                        vaultBrowserFragment.acmViewModel.clearModifiedPolicies()
                        val action = VaultBrowserFragmentDirections.actionVaultBrowserFragmentToAccessControlManagementFragment(vaultFile.file.absolutePath)
                        vaultBrowserFragment.findNavController().navigate(action)
                    }
                }
                is PeerVaultFileItem -> {
                    holder.container.tag = vaultFile

                    val dataCacheFile = vaultFile.getDataCacheFile(vaultBrowserFragment.requireContext())
                    if (!holder.setRemoteFileImage(vaultFile, dataCacheFile)) {
                        vaultFile.requestRemoteBitmap(holder)
                    }
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return files.size
    }

    fun updateItems(vaultFileItems: List<VaultFileItem>) {
        clearItems()
        files.addAll(vaultFileItems)
        notifyItemRangeInserted(0, vaultFileItems.size)
    }

    fun clearItems() {
        val count = itemCount
        files.clear()
        notifyItemRangeRemoved(0, count)
    }
}

class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val container: View = view.findViewById(R.id.container)
    private val iv: ImageView = view.findViewById(R.id.iv)
    val name: TextView = view.findViewById(R.id.fileName)
    private val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
    var inBitmap: Bitmap? = null

    fun setImageResource(resource: Int) {
        iv.setImageResource(resource)
        setLoading(false)
    }

    fun setImageBitmap(bitmap: Bitmap) {
        inBitmap = bitmap
        iv.setImageBitmap(bitmap)
        setLoading(false)
    }

    /***
     * Try to set the image bitmap if the vault file is still associated with this view holder.
     * If it is associated but the image can not be set, return false, else return true
     */
    fun setRemoteFileImage(peerVaultFileItem: PeerVaultFileItem, file: File): Boolean {
        if (container.tag == peerVaultFileItem) {
            val lastModified = file.lastModified()
            val currentTime = Date().time
            Log.e("Grid adapter", "last modified: ${file.lastModified()}, current time: $currentTime")

            if (file.exists() && file.canRead() && (currentTime - lastModified) < PeerVaultFileItem.CACHE_FILE_EXPIRY_MILLIS ) {
                try {
                    val options = BitmapFactory.Options().also {
                        it.outWidth = ImageViewHolder.IMAGE_WIDTH
                        it.inBitmap = inBitmap
                    }

                    val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    setImageBitmap(bitmap)
                } catch (e: Exception) {
                    val options = BitmapFactory.Options().also {
                        it.outWidth = ImageViewHolder.IMAGE_WIDTH
                    }

                    val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    setImageBitmap(bitmap)
                }
            } else {
                return false
            }
        }

        return true
    }

    fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            iv.visibility = View.GONE
            name.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
        } else {
            iv.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            // name is made visible only for folders
        }
    }

    companion object {
        const val IMAGE_WIDTH = 300
    }
}
