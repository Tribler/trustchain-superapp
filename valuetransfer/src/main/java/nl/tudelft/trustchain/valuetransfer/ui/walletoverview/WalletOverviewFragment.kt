package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import nl.tudelft.ipv8.android.IPv8Android
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.flow.map
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentWalletOverviewBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.BusinessIdentity
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.entity.PersonalIdentity
import nl.tudelft.trustchain.valuetransfer.entity.TestItem
import java.util.*

class WalletOverviewFragment : BaseFragment(R.layout.fragment_wallet_overview) {

    private val binding by viewBinding(FragmentWalletOverviewBinding::bind)

    private val adapter = ItemAdapter()

    private val store by lazy {
        IdentityStore.getInstance(requireContext())
    }

//    private val publicKeyBin by lazy {
//        requireArguments().getString(ARG_PUBLIC_KEY)!!
//    }

    private val publicKey by lazy {
        val publicKeyBin = IPv8Android.getInstance().myPeer.publicKey.toString()
        defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
    }

    private fun getCommunity() : IdentityCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("IdentityCommunity is not configured")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


//        val community = IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!

        // Register own key as trusted authority.
//        community.trustedAuthorityManager.addTrustedAuthority(IPv8Android.getInstance().myPeer.publicKey)

        Log.i("PUBLIC KEY:", IPv8Android.getInstance().myPeer.publicKey.toString())

        getCommunity().testCommunity()

//        getCommunity().deleteDatabase(this.context)

//        this.context?.deleteDatabase("identities-vt.db")

//        val personalIdentityContent = PersonalIdentity(
//            givenNames = "Joost",
//            surname = "Bambacht",
//            gender = "Male",
//            dateOfBirth = Date(),
//            placeOfBirth = "Den Bosch",
//            nationality = "Nederlands",
//            personalNumber = 123456789,
//            documentNumber = "2NH97G6PO",
//        )
//
//        val businessIdentityContent = BusinessIdentity(
//            companyName = "Alpha Chips Industries",
//            dateOfBirth = Date(),
//            residence = "The Hague",
//            areaOfExpertise = "Electronics",
//        )
//
//        var personalIdentity = Identity(
//            id = UUID.randomUUID().toString(),
//            type = "Personal",
//            publicKey = IPv8Android.getInstance().myPeer.publicKey,
//            content = personalIdentityContent,
//            added = Date(),
//            modified = Date(),
//        )
//
//        var businessIdentity = Identity(
//            id = UUID.randomUUID().toString(),
//            type = "Business",
//            publicKey = IPv8Android.getInstance().myPeer.publicKey,
//            content = businessIdentityContent,
//            added = Date(),
//            modified = Date(),
//        )
//
//        Log.d("IDENTITY:", personalIdentity.content.toString())
//        Log.d("IDENTITY:", businessIdentity.content.toString())

//        val personalIdentity = getCommunity().createIdentity("Personal")
//        store.addIdentity(personalIdentity)
//        val businessIdentity = getCommunity().createIdentity("Business")
//        store.addIdentity(businessIdentity)


//        val identity = getCommunity().createIdentity("A")
//        Log.d("CREATED", identity.givenNames.toString())

//
//
//        store.addIdentity(identity)

//        Log.i("PUBLIC KEYYY:", publicKey.toString())

//        adapter.registerRenderer(
//            IdentityItemRenderer{
//
//            }
//        )

        Log.i("TESTTTTTTTT", "AAAAAA")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    companion object {
        const val ARG_PUBLIC_KEY = "public_key"
        const val ARG_NAME = "name"

        private const val GROUP_TIME_LIMIT = 60 * 1000
        private const val PICK_IMAGE = 10
    }
}
