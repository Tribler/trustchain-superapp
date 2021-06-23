package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_identity.view.*
import kotlinx.android.synthetic.main.item_identity_detail.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import java.text.SimpleDateFormat

class IdentityItemRenderer(
    private val layoutType: Int,
    private val onLongItemClick: (Identity) -> Unit,
    private val onQRButtonClick: (Identity) -> Unit,
    private val onCopyPublicKeyButtonClick: (Identity) -> Unit,
) : ItemLayoutRenderer<IdentityItem, View>(
    IdentityItem::class.java
) {

    private val dateFormat = SimpleDateFormat("MMMM d, yyyy")

    override fun bindView(item: IdentityItem, view: View) = with(view) {
        if(layoutType == 0) {
            tvIdentityPublicKey.text = item.identity.publicKey.keyToBin().toHex()

            val content = item.identity.content
            tvIdentityGivenNames.text = content.givenNames
            tvIdentitySurname.text = content.surname

            setOnLongClickListener {
                onLongItemClick(item.identity)
                true
            }
        }else if(layoutType == 1) {
            val content = item.identity.content
            tvGivenNamesValue.text = content.givenNames
            tvSurnameValue.text = content.surname
            tvGenderValue.text = content.gender
            tvNationalityValue.text = content.nationality
            tvDateOfBirthValue.text = dateFormat.format(content.dateOfBirth)
            tvPlaceOfBirthValue.text = content.placeOfBirth
            tvPersonalNumberValue.text = content.personalNumber.toString()
            tvDocumentNumberValue.text = content.documentNumber
            tvPersonalPublicKeyValue.text = item.identity.publicKey.keyToBin().toHex()

            btnPersonalQRCode.setOnClickListener {
                onQRButtonClick(item.identity)
            }

            btnPersonalCopyPublicKey.setOnClickListener {
                onCopyPublicKeyButtonClick(item.identity)
            }

            setOnLongClickListener {
                onLongItemClick(item.identity)
                true
            }

        }
    }

    override fun getLayoutResourceId(): Int {
        when (layoutType) {
            0 -> return R.layout.item_identity
            1 -> return R.layout.item_identity_detail
        }

        return R.layout.item_identity
    }
}
