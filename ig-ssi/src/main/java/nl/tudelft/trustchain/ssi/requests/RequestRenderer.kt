package nl.tudelft.trustchain.ssi.requests

import android.annotation.SuppressLint
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_request.view.*
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.requests.RequestItem.Companion.ATTESTATION_REQUEST_ITEM
import nl.tudelft.trustchain.ssi.requests.RequestItem.Companion.VERIFY_REQUEST_ITEM

class RequestRenderer(
    private val onPositive: (RequestItem) -> Unit,
    private val onNegative: (RequestItem) -> Unit,
    private val onClick: ((RequestItem) -> Unit)? = null,
) : ItemLayoutRenderer<RequestItem, View>(
    RequestItem::class.java
) {

    @SuppressLint("SetTextI18n")
    override fun bindView(item: RequestItem, view: View): Unit = with(view) {
        when (item.requestType) {
            VERIFY_REQUEST_ITEM -> {
                headerText.text =
                    resources.getText(R.string.verifyRequest).toString() + item.attributeName
                positiveButton.text = resources.getText(R.string.allow)
                negativeButton.text = resources.getText(R.string.deny)
            }
            ATTESTATION_REQUEST_ITEM -> {
                headerText.text = resources.getText(R.string.attestationRequest)
                    .toString() + item.attributeName
                metadataHeaderText.visibility = View.VISIBLE
                metadataTextView.text = item.metadata
                metadataTextView.visibility = View.VISIBLE
                positiveButton.text = resources.getText(R.string.attest)
                negativeButton.text = resources.getText(R.string.dismiss)
            }
            else -> {
                throw RuntimeException("Invalid item in RequestRenderer.")
            }
        }

        peerMIDTextView.text = item.peer.mid

        positiveButton.setOnClickListener {
            onPositive(item)
        }

        negativeButton.setOnClickListener {
            onNegative(item)
        }

        setOnClickListener {
            onClick?.invoke(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_request
    }
}
