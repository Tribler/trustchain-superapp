package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity

class AttestationItemRenderer(
    private val parentActivity: ValueTransferMainActivity,
    private val onClickAction: (AttestationItem) -> Unit,
    private val onLongClickAction: (AttestationItem) -> Unit,
) : ItemLayoutRenderer<AttestationItem, View>(
    AttestationItem::class.java
) {

    override fun bindView(item: AttestationItem, view: View) = with(view) {
//        if (item.attestationBlob.metadata != null) {
//            val metadata = JSONObject(item.attestationBlob.metadata!!)
//            tvAttestationNameValue.apply {
//                text = StringBuilder()
//                    .append(metadata.optString("attribute"))
//                    .append(": ")
//                    .append(metadata.optString("value"))
//                setTextColor(
//                    ContextCompat.getColor(
//                        this.context,
//                        getColorIDFromThemeAttribute(parentActivity, R.attr.onWidgetColor)
//                    )
//                )
//            }
//
//            tvAttestationIDFormat.text = StringBuilder()
//                .append("(")
//                .append(item.attestationBlob.idFormat)
//                .append(")")
//
//            ivAttestationQRButton.isVisible = true
//            ivAttestationDelete.isVisible = false
//
//            ivAttestationQRButton.setOnClickListener {
//                onClickAction(item)
//            }
//        } else {
//            tvAttestationNameValue.apply {
//                text = this.context.resources.getString(R.string.text_attestation_malformed)
//                setTextColor(Color.RED)
//            }
//
//            ivAttestationQRButton.isVisible = false
//            ivAttestationDelete.isVisible = true
//
//            ivAttestationDelete.setOnClickListener {
//                onClickAction(item)
//            }
//        }
//
//        tvAttestationHash.text = item.attestationBlob.attestationHash.toHex()
//        tvAttestationBlob.text = item.attestationBlob.blob.toHex()

        setOnLongClickListener {
            onLongClickAction(item)
            true
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_identity_attestation
    }
}
