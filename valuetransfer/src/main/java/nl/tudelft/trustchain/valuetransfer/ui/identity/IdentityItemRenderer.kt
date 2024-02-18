package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.ItemIdentityBinding
import nl.tudelft.trustchain.valuetransfer.databinding.ItemIdentityDetailBinding
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.entity.PersonalIdentity
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon
import java.text.SimpleDateFormat
import java.util.Locale

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
        val binding = if (layoutType == 0) {
            ItemIdentityBinding.bind(view)
        } else {
            ItemIdentityDetailBinding.bind(view)
        }

        val publicKeyString = item.identity.publicKey.keyToBin().toHex()

        if (layoutType == 0) {
            binding as ItemIdentityBinding
            binding.tvIdentityPublicKey.text = item.identity.publicKey.keyToBin().toHex()

            item.identity.content.let { content ->
                binding.tvIdentityGivenNamesSurname.text =
                    StringBuilder()
                        .append(content.givenNames)
                        .append(" ")
                        .append(content.surname)
            }

            binding.ivContactVerifiedStatus.isVisible = item.identity.content.verified
            binding.ivContactUnverifiedStatus.isVisible = !item.identity.content.verified

            binding.flIdenticon.background = if (item.connected) {
                ContextCompat.getDrawable(view.context, R.drawable.pill_rounded_green)
            } else ContextCompat.getDrawable(view.context, R.drawable.pill_rounded_red)

            if (item.image != null) {
                binding.ivIdentityPhoto.setImageBitmap(item.image)
                binding.ivIdentityPhoto.isVisible = true
            } else {
                generateIdenticon(
                    publicKeyString.substring(20, publicKeyString.length).toByteArray(),
                    getColorByHash(context, publicKeyString),
                    resources
                ).let { identicon ->
                    binding.ivIdenticon.setImageBitmap(identicon)
                    binding.ivIdenticon.isVisible = true
                }
            }

            binding.btnScanIdentityQR.setOnClickListener {
                onQRButtonClick(item.identity)
            }

            view.setOnClickListener {
                onIdentityImageClick(item.identity)
            }
        } else if (layoutType == 1) {
            binding as ItemIdentityDetailBinding
            val content = item.identity.content

            binding.tvGivenNamesSurnameValue.text =
                StringBuilder()
                    .append(content.givenNames)
                    .append(" ")
                    .append(content.surname)

            setContentVisible(view, content, false)

            binding.tvPublicKeyValue.apply {
                text = publicKeyString
                setOnClickListener {
                    when (this.lineCount) {
                        3 -> binding.tvPublicKeyValue.maxLines = 10
                        else -> binding.tvPublicKeyValue.maxLines = 3
                    }
                }
            }

            if (item.image != null) {
                binding.ivIdentityIdenticon.isVisible = false
                binding.ivIdentityImage.setImageBitmap(item.image)
                binding.ivIdentityImage.isVisible = true
            } else {
                generateIdenticon(
                    publicKeyString.substring(20, publicKeyString.length).toByteArray(),
                    getColorByHash(context, publicKeyString),
                    resources
                ).let { identicon ->
                    binding.ivIdentityImage.isVisible = false
                    binding.ivIdentityIdenticon.setImageBitmap(identicon)
                    binding.ivIdentityIdenticon.isVisible = true
                }
            }

            binding.ivShowDetails.setOnClickListener {
                setContentVisible(view, content, true)
                binding.ivShowDetails.isVisible = false
                binding.ivHideDetails.isVisible = true
            }

            binding.ivHideDetails.setOnClickListener {
                setContentVisible(view, content, false)
                binding.ivShowDetails.isVisible = true
                binding.ivHideDetails.isVisible = false
            }

            binding.flShowMoreLess.setOnClickListener {
                binding.rlRowExtra.isVisible = !binding.rlRowExtra.isVisible
                binding.ivShowMoreIcon.isVisible = !binding.rlRowExtra.isVisible
                binding.ivShowLessIcon.isVisible = binding.rlRowExtra.isVisible

                binding.clShowHideDetails.isVisible = binding.rlRowExtra.isVisible
                binding.ivShowDetails.isVisible = true
                binding.ivHideDetails.isVisible = false

                setContentVisible(view, content, false)
            }

            binding.rlStatusVerified.isVisible = item.identity.content.verified
            binding.rlStatusNotVerified.isVisible = !item.identity.content.verified

            binding.btnQRCode.setOnClickListener {
                onQRButtonClick(item.identity)
            }

            binding.btnCopyPublicKey.setOnClickListener {
                onCopyPublicKeyButtonClick(item.identity)
            }

            binding.cvIdentityImage.setOnClickListener {
                onIdentityImageClick(item.identity)
            }
        }
    }

    private fun setContentVisible(view: View, content: PersonalIdentity, visible: Boolean) =
        with(view) {
            val binding = ItemIdentityDetailBinding.bind(view)
            val dummy = "*".repeat(8)

            binding.tvGenderValue.text = if (visible) content.gender else dummy
            binding.tvDatePlaceOfBirthValue.text =
                if (visible) dateFormat.format(content.dateOfBirth) else dummy
            binding.tvDateExpiryValue.text =
                if (visible) dateFormat.format(content.dateOfExpiry) else dummy
            binding.tvNationalityValue.text = if (visible) content.nationality else dummy
            binding.tvPersonalNumberValue.text =
                if (visible) content.personalNumber.toString() else dummy
            binding.tvDocumentNumberValue.text =
                if (visible) content.documentNumber else dummy
        }

    override fun getLayoutResourceId(): Int {
        return when (layoutType) {
            0 -> R.layout.item_identity
            1 -> R.layout.item_identity_detail
            else -> R.layout.item_identity
        }
    }
}
