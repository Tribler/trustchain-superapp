package nl.tudelft.trustchain.ssi.database

import android.annotation.SuppressLint
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_database.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.ssi.R
import org.json.JSONObject

class DatabaseItemRenderer(
    private val onItemClick: (DatabaseItem) -> Unit,
    private val onRemoveButtonClick: (DatabaseItem) -> Unit,
) : ItemLayoutRenderer<DatabaseItem, View>(
    DatabaseItem::class.java
) {

    @SuppressLint("SetTextI18n")
    override fun bindView(item: DatabaseItem, view: View) = with(view) {
        if (item.attestationBlob.metadata != null) {
            val metadata = JSONObject(item.attestationBlob.metadata!!)
            attributeNameAndValue.text =
                metadata.optString("attribute") + ": " + metadata.optString("value")
        } else {
            attributeNameAndValue.text = "ATTRIBUTE INFORMATION NOT STORED"
        }
        hash.text = item.attestationBlob.attestationHash.copyOfRange(0, 20).toHex()
        idformat.text = item.attestationBlob.idFormat
        blob.text = item.attestationBlob.blob.copyOfRange(0, 20).toHex()

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
