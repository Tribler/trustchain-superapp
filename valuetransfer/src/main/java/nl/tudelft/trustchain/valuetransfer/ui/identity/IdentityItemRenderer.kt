package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.*
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_identity.view.*
import kotlinx.android.synthetic.main.item_identity_detail.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.entity.PersonalIdentity
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon
import java.text.SimpleDateFormat
import java.util.*

class IdentityItemRenderer(
    private val layoutType: Int,
    private val onQRButtonClick: (Identity) -> Unit,
    private val onCopyPublicKeyButtonClick: (Identity) -> Unit,
    private val onIdentityImageClick: (Identity) -> Unit,
) : ItemLayoutRenderer<IdentityItem, View>(
    IdentityItem::class.java
) {
    private val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)

    override fun bindView(item: IdentityItem, view: View) = with(view) {
        val publicKeyString = item.identity.publicKey.keyToBin().toHex()

        if (layoutType == 0) {
            tvIdentityPublicKey.text = item.identity.publicKey.keyToBin().toHex()

            item.identity.content.let { content ->
                tvIdentityGivenNamesSurname.text =
                    StringBuilder()
                        .append(content.givenNames)
                        .append(" ")
                        .append(content.surname)
            }

            ivContactVerifiedStatus.isVisible = item.identity.content.verified
            ivContactUnverifiedStatus.isVisible = !item.identity.content.verified

            flIdenticon.background = if (item.connected) {
                ContextCompat.getDrawable(view.context, R.drawable.pill_rounded_green)
            } else ContextCompat.getDrawable(view.context, R.drawable.pill_rounded_red)

            if (item.image != null) {
                ivIdentityPhoto.setImageBitmap(item.image)
                ivIdentityPhoto.isVisible = true
            } else {
                generateIdenticon(
                    publicKeyString.substring(20, publicKeyString.length).toByteArray(),
                    getColorByHash(context, publicKeyString),
                    resources
                ).let { identicon ->
                    ivIdenticon.setImageBitmap(identicon)
                    ivIdenticon.isVisible = true
                }
            }

            btnScanIdentityQR.setOnClickListener {
                onQRButtonClick(item.identity)
            }

            view.setOnClickListener {
                onIdentityImageClick(item.identity)
            }
        } else if (layoutType == 1) {
            val content = item.identity.content

            tvGivenNamesSurnameValue.text =
                StringBuilder()
                    .append(content.givenNames)
                    .append(" ")
                    .append(content.surname)

            setContentVisible(view, content, false)

            tvPublicKeyValue.apply {
                text = publicKeyString
                setOnClickListener {
                    when (this.lineCount) {
                        3 -> tvPublicKeyValue.maxLines = 10
                        else -> tvPublicKeyValue.maxLines = 3
                    }
                }
            }

            if (item.image != null) {
                ivIdentityIdenticon.isVisible = false
                ivIdentityImage.setImageBitmap(item.image)
                ivIdentityImage.isVisible = true
            } else {
                generateIdenticon(
                    publicKeyString.substring(20, publicKeyString.length).toByteArray(),
                    getColorByHash(context, publicKeyString),
                    resources
                ).let { identicon ->
                    ivIdentityImage.isVisible = false
                    ivIdentityIdenticon.setImageBitmap(identicon)
                    ivIdentityIdenticon.isVisible = true
                }
            }

            ivShowDetails.setOnClickListener {
                setContentVisible(view, content, true)
                ivShowDetails.isVisible = false
                ivHideDetails.isVisible = true
            }

            ivHideDetails.setOnClickListener {
                setContentVisible(view, content, false)
                ivShowDetails.isVisible = true
                ivHideDetails.isVisible = false
            }

            flShowMoreLess.setOnClickListener {
                rlRowExtra.isVisible = !rlRowExtra.isVisible
                ivShowMoreIcon.isVisible = !rlRowExtra.isVisible
                ivShowLessIcon.isVisible = rlRowExtra.isVisible

                clShowHideDetails.isVisible = rlRowExtra.isVisible
                ivShowDetails.isVisible = true
                ivHideDetails.isVisible = false

                setContentVisible(view, content, false)
            }

            rlStatusVerified.isVisible = item.identity.content.verified
            rlStatusNotVerified.isVisible = !item.identity.content.verified

            btnQRCode.setOnClickListener {
                onQRButtonClick(item.identity)
            }

            btnCopyPublicKey.setOnClickListener {
                onCopyPublicKeyButtonClick(item.identity)
            }

            cvIdentityImage.setOnClickListener {
                onIdentityImageClick(item.identity)
            }
        }
    }

    private fun setContentVisible(view: View, content: PersonalIdentity, visible: Boolean) = with(view) {
        val dummy = "*".repeat(8)
        tvGenderValue.text = if (visible) content.gender else dummy
        tvDatePlaceOfBirthValue.text = if (visible) dateFormat.format(content.dateOfBirth) else dummy
        tvDateExpiryValue.text = if (visible) dateFormat.format(content.dateOfExpiry) else dummy
        tvNationalityValue.text = if (visible) content.nationality else dummy
        tvPersonalNumberValue.text = if (visible) content.personalNumber.toString() else dummy
        tvDocumentNumberValue.text = if (visible) content.documentNumber else dummy
    }

    override fun getLayoutResourceId(): Int {
        when (layoutType) {
            0 -> return R.layout.item_identity
            1 -> return R.layout.item_identity_detail
        }

        return R.layout.item_identity
    }
}
