package nl.tudelft.trustchain.datavault.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.trustchain.datavault.R

class BrowserGridAdapter(
    private val context: Context,
    private val vaultBrowserFragment: VaultBrowserFragment,
    private var files: List<VaultFileItem>
    ): RecyclerView.Adapter<ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(LayoutInflater.from(context).inflate(R.layout.browser_grid_item, parent, false))
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val vaultFile = files[position]

        holder.name.text = vaultFile.file.name

        if (vaultFile.file.isDirectory) {
            holder.iv.setImageResource(R.mipmap.folder_icon)
            holder.container.setOnClickListener {
                vaultBrowserFragment.navigateToFolder(vaultFile.file)
            }
        } else {
            holder.iv.setImageURI(vaultFile.file.toUri())

            holder.container.setOnClickListener {
                vaultBrowserFragment.acmViewModel.clearModifiedPolicies()
                val action = VaultBrowserFragmentDirections.actionVaultBrowserFragmentToAccessControlManagementFragment(vaultFile.file.absolutePath)
                vaultBrowserFragment.findNavController().navigate(action)
            }
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
}
