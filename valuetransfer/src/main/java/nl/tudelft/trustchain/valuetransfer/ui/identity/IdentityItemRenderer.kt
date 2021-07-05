package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.view.View
import androidx.navigation.findNavController
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_identity_new.view.*
import kotlinx.android.synthetic.main.item_identity_detail_new.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import java.text.SimpleDateFormat

class IdentityItemRenderer(
    private val layoutType: Int,
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
            tvIdentityGivenNamesSurname.text = "${content.givenNames} ${content.surname}"

            view.setOnClickListener {
                findNavController().navigate(R.id.action_identityOverview_to_identityView)
            }

        }else if(layoutType == 1) {
            val content = item.identity.content

            tvGivenNamesSurnameValue.text = "${content.givenNames} ${content.surname}"
            tvGenderValue.text = content.gender
            tvDatePlaceOfBirthValue.text = "${dateFormat.format(content.dateOfBirth)}, ${content.placeOfBirth}"
            tvNationalityValue.text = content.nationality
            tvPersonalNumberValue.text = content.personalNumber.toString()
            tvDocumentNumberValue.text = content.documentNumber
            tvPublicKeyValue.text = item.identity.publicKey.keyToBin().toHex()

            tvPublicKeyValue.setOnClickListener {

                when(tvPublicKeyValue.lineCount) {
                    2 -> tvPublicKeyValue.maxLines = 6
                    else -> tvPublicKeyValue.maxLines = 2
                }
            }

            btnPersonalQRCode.setOnClickListener {
                onQRButtonClick(item.identity)
            }

            btnPersonalCopyPublicKey.setOnClickListener {
                onCopyPublicKeyButtonClick(item.identity)
            }
        }
    }

    override fun getLayoutResourceId(): Int {
        when (layoutType) {
            0 -> return R.layout.item_identity_new
            1 -> return R.layout.item_identity_detail_new
        }

        return R.layout.item_identity
    }
}
