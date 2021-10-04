package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_identity.view.*
import kotlinx.android.synthetic.main.item_identity.view.ivIdenticon
import kotlinx.android.synthetic.main.item_identity_detail.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon
import java.text.SimpleDateFormat
import java.util.*

class IdentityItemRenderer(
    private val layoutType: Int,
    private val onQRButtonClick: (Identity) -> Unit,
    private val onCopyPublicKeyButtonClick: (Identity) -> Unit,
) : ItemLayoutRenderer<IdentityItem, View>(
    IdentityItem::class.java
) {

    private val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)

    override fun bindView(item: IdentityItem, view: View) = with(view) {
        if (layoutType == 0) {
            tvIdentityPublicKey.text = item.identity.publicKey.keyToBin().toHex()

            item.identity.content.let { content ->
                tvIdentityGivenNamesSurname.text =
                    StringBuilder()
                        .append(content.givenNames)
                        .append(" ")
                        .append(content.surname)
            }

            item.identity.publicKey.toString().let { publicKeyString ->
                generateIdenticon(
                    publicKeyString.substring(20, publicKeyString.length).toByteArray(),
                    getColorByHash(context, publicKeyString),
                    resources
                ).let {
                    ivIdenticon.setImageBitmap(it)
                }
            }

            view.setOnClickListener {
                onQRButtonClick(item.identity)
            }
        } else if (layoutType == 1) {
            item.identity.content.let { content ->
                tvGivenNamesSurnameValue.text =
                    StringBuilder()
                        .append(content.givenNames)
                        .append(" ")
                        .append(content.surname)
                tvGenderValue.text = content.gender
                tvDatePlaceOfBirthValue.text =
                    StringBuilder().append(dateFormat.format(content.dateOfBirth))
                        .append(", ")
                        .append(content.placeOfBirth)
                tvNationalityValue.text = content.nationality
                tvPersonalNumberValue.text = content.personalNumber.toString()
                tvDocumentNumberValue.text = content.documentNumber
            }

            tvPublicKeyValue.apply {
                text = item.identity.publicKey.keyToBin().toHex()
                setOnClickListener {
                    when (this.lineCount) {
                        3 -> tvPublicKeyValue.maxLines = 6
                        else -> tvPublicKeyValue.maxLines = 3
                    }
                }
            }

            btnQRCode.setOnClickListener {
                onQRButtonClick(item.identity)
            }

            btnCopyPublicKey.setOnClickListener {
                onCopyPublicKeyButtonClick(item.identity)
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
