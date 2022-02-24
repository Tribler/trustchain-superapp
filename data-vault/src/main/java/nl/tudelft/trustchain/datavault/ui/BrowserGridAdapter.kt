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
    private var files: List<VaultFileItem>
    ): RecyclerView.Adapter<ImageViewHolder>() {
    var requested = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(LayoutInflater.from(vaultBrowserFragment.requireContext()).inflate(R.layout.browser_grid_item, parent, false))
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.setLoading(true)

        val vaultFile = files[position]

        holder.name.text = vaultFile.name

        if (vaultFile is LocalVaultFileItem) {
            //holder.inBitmap = vaultFile.getImage(holder.inBitmap)
            val bitmap = vaultFile.getImage(holder.inBitmap)
            holder.iv.setImageBitmap(bitmap)
            holder.setLoading(false)
        } else if (vaultFile is PeerVaultFileItem) {
            holder.container.tag = vaultFile

            val dataCacheFile = vaultFile.getDataCacheFile(vaultBrowserFragment.requireContext())
            if (!holder.setRemoteFileImage(vaultFile, dataCacheFile)) {
                if (requested < 10) {
                    vaultFile.requestRemoteBitmap(holder)
                    requested++
                }
            }
        }

        if (vaultFile.isDirectory()) {
            //holder.iv.setImageResource(R.mipmap.folder_icon)
            holder.container.setOnClickListener {
                vaultBrowserFragment.navigateToFolder(vaultFile)
            }
        } else if (vaultFile is LocalVaultFileItem) {
            holder.container.setOnClickListener {
                vaultBrowserFragment.acmViewModel.clearModifiedPolicies()
                val action = VaultBrowserFragmentDirections.actionVaultBrowserFragmentToAccessControlManagementFragment(vaultFile.file.absolutePath)
                vaultBrowserFragment.findNavController().navigate(action)
            }
        } else {
            holder.container.setOnClickListener(null)
        }

    }

    override fun getItemCount(): Int {
        return files.size
    }

    fun updateItems(vaultFileItems: List<VaultFileItem>) {
        files = vaultFileItems
        notifyDataSetChanged()
    }
}

class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val container: View = view.findViewById(R.id.container)
    val iv: ImageView = view.findViewById(R.id.iv)
    val name: TextView = view.findViewById(R.id.fileName)
    private val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
    var inBitmap: Bitmap? = null

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
                val options = BitmapFactory.Options().also {
                    //it.outWidth = VaultFileItem.IMAGE_WIDTH
                    it.inSampleSize = 6
                }
                inBitmap?.also {
                    //options.inBitmap = inBitmap
                }
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    Log.e("Grid Adapter", "decoding remote cache file (${file.length()} bytes) $bitmap")
                    //inBitmap = Bitmap.createScaledBitmap(bitmap, VaultFileItem.IMAGE_WIDTH, VaultFileItem.IMAGE_WIDTH, true)
                    iv.setImageBitmap(bitmap)
                    setLoading(false)
                } catch (e: Exception) {
                    Log.e("Grid Adapter", "decoding remote cache file failed")
                    Log.e("Grid Adapter", e.toString())
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
            name.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }
}
