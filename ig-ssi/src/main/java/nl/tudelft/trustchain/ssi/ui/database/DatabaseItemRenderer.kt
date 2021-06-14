package nl.tudelft.trustchain.ssi.ui.database

import android.annotation.SuppressLint
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_database.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.util.decodeImage
import nl.tudelft.trustchain.ssi.ui.dialogs.attestation.ID_PICTURE
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        if (attestation.attributeName == ID_PICTURE.toUpperCase(Locale.getDefault())) {
            pictureView.setImageBitmap(decodeImage(String(attestation.attributeValue)))
            pictureView.visibility = View.VISIBLE
        }
        attributeNameAndValue.text =
            attestation.attributeName + ": " + String(attestation.attributeValue)
        // } else {
        //     attributeNameAndValue.text = "MALFORMED ATTESTATION"
        //     attributeNameAndValue.setTextColor(Color.RED)
        // }
        hash.text = attestation.attributeHash.toHex()
        idformat.text = attestation.idFormat
        blob.text = SimpleDateFormat(
            "MM/dd/yyyy",
            Locale.getDefault()
        ).format(Date(attestation.signDate.toLong() * 1000)).toString()

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
