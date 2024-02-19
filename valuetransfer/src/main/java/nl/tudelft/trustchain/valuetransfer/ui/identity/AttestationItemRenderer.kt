package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.ItemIdentityAttestationBinding
import nl.tudelft.trustchain.valuetransfer.util.getColorIDFromThemeAttribute
import org.json.JSONObject

class AttestationItemRenderer(
    private val parentActivity: ValueTransferMainActivity,
    private val onClickAction: (AttestationItem) -> Unit,
    private val onLongClickAction: (AttestationItem) -> Unit,
) : ItemLayoutRenderer<AttestationItem, View>(
        AttestationItem::class.java
    ) {
    override fun bindView(
        item: AttestationItem,
        view: View
    ) = with(view) {
        val binding = ItemIdentityAttestationBinding.bind(view)
        if (item.attestationBlob.metadata != null) {
            val metadata = JSONObject(item.attestationBlob.metadata!!)
            binding.tvAttestationNameValue.apply {
                text =
                    StringBuilder()
                        .append(metadata.optString("attribute"))
                        .append(": ")
                        .append(metadata.optString("value"))
                setTextColor(
                    ContextCompat.getColor(
                        this.context,
                        getColorIDFromThemeAttribute(parentActivity, R.attr.onWidgetColor)
                    )
                )
            }

            binding.tvAttestationIDFormat.text =
                StringBuilder()
                    .append("(")
                    .append(item.attestationBlob.idFormat)
                    .append(")")

            binding.ivAttestationQRButton.isVisible = true
            binding.ivAttestationDelete.isVisible = false

            binding.ivAttestationQRButton.setOnClickListener {
                onClickAction(item)
            }
        } else {
            binding.tvAttestationNameValue.apply {
                text = this.context.resources.getString(R.string.text_attestation_malformed)
                setTextColor(Color.RED)
            }

            binding.ivAttestationQRButton.isVisible = false
            binding.ivAttestationDelete.isVisible = true

            binding.ivAttestationDelete.setOnClickListener {
                onClickAction(item)
            }
        }

        binding.tvAttestationHash.text = item.attestationBlob.attestationHash.toHex()
        binding.tvAttestationBlob.text = item.attestationBlob.blob.toHex()

        setOnLongClickListener {
            onLongClickAction(item)
            true
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_identity_attestation
    }
}
