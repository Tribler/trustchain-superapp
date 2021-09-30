package nl.tudelft.trustchain.datavault.ui

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.vault_file_item.view.*
import nl.tudelft.trustchain.datavault.R

class VaultFileItemRenderer : ItemLayoutRenderer<VaultFileItem, View>(VaultFileItem::class.java){
    override fun bindView(item: VaultFileItem, view: View) = with(view) {
        fileName.text = item.file.name
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.vault_file_item
    }

}
