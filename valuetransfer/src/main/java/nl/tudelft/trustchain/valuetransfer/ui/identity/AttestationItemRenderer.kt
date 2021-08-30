package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.graphics.Color
import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_identity_attestation.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.ssi.database.DatabaseItem
import nl.tudelft.trustchain.valuetransfer.R
import org.json.JSONObject

class AttestationItemRenderer(
    private val onClickAction: (AttestationItem) -> Unit,
    private val onLongClickAction: (AttestationItem) -> Unit,
) : ItemLayoutRenderer<AttestationItem, View>(
    AttestationItem::class.java
) {

    override fun bindView(item: AttestationItem, view: View) = with(view) {
        if (item.attestationBlob.metadata != null) {
            val metadata = JSONObject(item.attestationBlob.metadata!!)
            tvAttestationNameValue.text = metadata.optString("attribute") + ": " + metadata.optString("value")
            tvAttestationNameValue.setTextColor(Color.WHITE)

            tvAttestationIDFormat.text = "(" + item.attestationBlob.idFormat + ")"

            ivAttestationQRButton.isVisible = true
            ivAttestationDelete.isVisible = false


            ivAttestationQRButton.setOnClickListener {
                onClickAction(item)
            }
        } else {
            tvAttestationNameValue.text = "MALFORMED ATTESTATION"
            tvAttestationNameValue.setTextColor(Color.RED)

            ivAttestationQRButton.isVisible = false
            ivAttestationDelete.isVisible = true

            ivAttestationDelete.setOnClickListener {
                onClickAction(item)
            }
        }

        tvAttestationHash.text = item.attestationBlob.attestationHash.toHex()
        tvAttestationBlob.text = item.attestationBlob.blob.toHex()

        setOnLongClickListener {
            onLongClickAction(item)
            true
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_identity_attestation
    }
}
