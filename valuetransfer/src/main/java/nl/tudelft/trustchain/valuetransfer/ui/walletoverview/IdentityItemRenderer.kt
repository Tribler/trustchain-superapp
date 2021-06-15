package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_identity.view.*
import kotlinx.android.synthetic.main.item_identity_business_detail.view.*
import kotlinx.android.synthetic.main.item_identity_personal_detail.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.entity.BusinessIdentity
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.entity.PersonalIdentity
import java.text.SimpleDateFormat

class IdentityItemRenderer(
    private val layoutType: Int,
    private val onItemClick: (Identity) -> Unit,
//    private val onItemLongClick: (Contact) -> Unit
) : ItemLayoutRenderer<IdentityItem, View>(
    IdentityItem::class.java
) {

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy")
    private val yearFormat = SimpleDateFormat("yyyy")

    override fun bindView(item: IdentityItem, view: View) = with(view) {
        if(layoutType == 0) {
            tvIdentityPublicKey.text = item.identity.publicKey.toString()
            tvIdentityType.text = item.identity.type

            if(item.identity.type == "Personal") {
                val content = item.identity.content as PersonalIdentity
                tvIdentityGivenNames.text = content.givenNames
                tvIdentitySurname.text = content.surname
            }else if(item.identity.type == "Business") {
                val content = item.identity.content as BusinessIdentity
                tvIdentityGivenNames.text = content.companyName
            }

            setOnClickListener {
                onItemClick(item.identity)
            }
        }else if(layoutType == 1) {
            val content = item.identity.content as PersonalIdentity
            tvGivenNamesValue.text = content.givenNames
            tvSurnameValue.text = content.surname
            tvGenderValue.text = content.gender
            tvNationalityValue.text = content.nationality
            tvDateOfBirthValue.text = dateFormat.format(content.dateOfBirth)
            tvPlaceOfBirthValue.text = content.placeOfBirth
            tvPersonalNumberValue.text = content.personalNumber.toString()
            tvDocumentNumberValue.text = content.documentNumber
            tvPersonalPublicKeyValue.text = item.identity.publicKey.toString()

//            setOnClickListener {
//                onItemClick(item.identity)
//            }

            btnQRCode.
        }else if(layoutType == 2) {
            val content = item.identity.content as BusinessIdentity
            tvCompanyNameValue.text = content.companyName
            tvESTValue.text = yearFormat.format(content.dateOfBirth)
            tvResidenceValue.text = content.residence
            tvAreaOfExpertiseValue.text = content.areaOfExpertise
            tvBusinessPublicKeyValue.text = item.identity.publicKey.toString()

            setOnClickListener {
                onItemClick(item.identity)
            }
        }
//
//        setOnLongClickListener {
//            onItemLongClick(item.identity)
//            true
//        }
    }

    override fun getLayoutResourceId(): Int {
        when (layoutType) {
            0 -> return R.layout.item_identity
            1 -> return R.layout.item_identity_personal_detail
            2 -> return R.layout.item_identity_business_detail
        }

        return R.layout.item_identity
    }
}
