package nl.tudelft.trustchain.ssi.database

import android.annotation.SuppressLint
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_database.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.ssi.R

class DatabaseItemRenderer(
    private val onItemClick: (DatabaseItem) -> Unit,
    private val onRemoveButtonClick: (DatabaseItem) -> Unit,
) : ItemLayoutRenderer<DatabaseItem, View>(
    DatabaseItem::class.java
) {

    @SuppressLint("SetTextI18n")
    override fun bindView(item: DatabaseItem, view: View) = with(view) {
        val attestation = item.attestation
        // if (attestation.attributeValue != null) {
        attributeNameAndValue.text =
            attestation.attributeName + ": " + attestation.attributeValue
        // } else {
        //     attributeNameAndValue.text = "MALFORMED ATTESTATION"
        //     attributeNameAndValue.setTextColor(Color.RED)
        // }
        hash.text = attestation.attributeHash.toHex()
        idformat.text = attestation.idFormat
        blob.text = "Remove this"

        removeButton.setOnClickListener {
            onRemoveButtonClick(item)
        }

        setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_database
    }
}
