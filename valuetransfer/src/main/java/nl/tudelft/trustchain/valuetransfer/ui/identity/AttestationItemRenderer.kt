package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.graphics.Color
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_identity_attestation.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.ssi.database.DatabaseItem
import nl.tudelft.trustchain.valuetransfer.R
import org.json.JSONObject

class AttestationItemRenderer(
    private val onClickAction: (DatabaseItem) -> Unit,
    private val onLongClickAction: (DatabaseItem) -> Unit,
) : ItemLayoutRenderer<DatabaseItem, View>(
    DatabaseItem::class.java
) {

    override fun bindView(item: DatabaseItem, view: View) = with(view) {
        if (item.attestationBlob.metadata != null) {
            val metadata = JSONObject(item.attestationBlob.metadata!!)
            tvAttestationNameValue.text = metadata.optString("attribute") + ": " + metadata.optString("value")
        } else {
            tvAttestationNameValue.text = "MALFORMED ATTESTATION"
            tvAttestationNameValue.setTextColor(Color.RED)
        }

        tvAttestationHash.text = item.attestationBlob.attestationHash.copyOfRange(0, 20).toHex()
        tvAttestationIDFormat.text = "(" + item.attestationBlob.idFormat + ")"
        tvAttestationBlob.text = item.attestationBlob.blob.copyOfRange(0, 20).toHex()

        ivAttestationQRButton.setOnClickListener {
            onClickAction(item)
        }

        setOnLongClickListener {
            onLongClickAction(item)
            true
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_identity_attestation
    }
}
