package nl.tudelft.trustchain.datavault.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import nl.tudelft.trustchain.datavault.R

class PhotoGridAdapter(
    private val context: Context,
    private val fragment: Fragment,
    private var images: List<VaultFileItem>
    ): RecyclerView.Adapter<ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(LayoutInflater.from(context).inflate(R.layout.photo_grid_item, parent, false))
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val vaultFile = images[position]

        holder.iv.setImageURI(vaultFile.file.toUri())

        holder.iv.setOnClickListener {
            val action = VaultBrowserFragmentDirections.actionVaultBrowserFragmentToAccessControlManagementFragment(vaultFile.file.absolutePath)
            fragment.findNavController().navigate(action)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    fun updateItems(vaultFileItems: List<VaultFileItem>) {
        images = vaultFileItems
    }
}

class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val iv = view.findViewById<ImageView>(R.id.iv)
}
