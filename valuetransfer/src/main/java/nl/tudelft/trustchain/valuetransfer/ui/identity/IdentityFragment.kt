package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.schema.SchemaManager
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.valuetransfer.util.copyToClipboard
import nl.tudelft.trustchain.valuetransfer.util.mapToJSON
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.ssi.database.DatabaseItem
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentIdentityBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.dialogs.ConfirmDialog
import nl.tudelft.trustchain.valuetransfer.dialogs.IdentityAttributeAddDialog
import nl.tudelft.trustchain.valuetransfer.dialogs.IdentityDetailsDialog
import nl.tudelft.trustchain.valuetransfer.dialogs.QRCodeDialog
import nl.tudelft.trustchain.valuetransfer.entity.Attribute
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import org.json.JSONObject

class IdentityFragment : BaseFragment(R.layout.fragment_identity) {

    private val binding by viewBinding(FragmentIdentityBinding::bind)
    private val adapterIdentity = ItemAdapter()
    private val adapterAttributes = ItemAdapter()
    private val adapterAttestations = ItemAdapter()

    private val itemsIdentity: LiveData<List<Item>> by lazy {
        identityStore.getAllIdentities().map { identities ->
            createIdentityItems(identities)
        }.asLiveData()
    }

    private val itemsAttributes: LiveData<List<Item>> by lazy {
        identityStore.getAllAttributes().map { attributes ->
            createAttributeItems(attributes)
        }.asLiveData()
    }

    private val identityStore by lazy {
        IdentityStore.getInstance(requireContext())
    }

    private fun attestationCommunity() : AttestationCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("AttestationCommunity is not configured")
    }

    private fun identityCommunity(): IdentityCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("IdentityCommunity is not configured")
    }

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_identity, container, false)
    }

    override fun onResume() {
        super.onResume()

        (requireActivity() as ValueTransferMainActivity).setActionBarTitle("Identity")
        (requireActivity() as ValueTransferMainActivity).toggleActionBar(false)
        (requireActivity() as ValueTransferMainActivity).toggleBottomNavigation(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onResume()

        adapterIdentity.registerRenderer(
            IdentityItemRenderer(
                1,
                 { identity ->
                    val map = mapOf(
                        "public_key" to identity.publicKey.keyToBin().toHex(),
                        "message" to "TEST"
                    )
                    QRCodeDialog("Personal Public Key", "Show QR-code to other party", mapToJSON(map).toString())
                        .show(parentFragmentManager, tag)
                }, { identity ->
                    copyToClipboard(requireContext(), identity.publicKey.keyToBin().toHex(), "Public Key")
                }
            )
        )

        adapterAttributes.registerRenderer(
            AttributeItemRenderer(
                {
                    IdentityAttributeAddDialog(it, getUnusedAttributeNames(), identityCommunity()).show(parentFragmentManager, tag)
                },
                {
                    Log.d("TESTJE", "SHARE ATTRIBUTE")
                }
            )
        )

        adapterAttestations.registerRenderer(
            AttestationItemRenderer(
                {
                    if(it.attestationBlob.signature != null) {
                        val manager = SchemaManager()
                        manager.registerDefaultSchemas()
                        val attestation = manager.deserialize(it.attestationBlob.blob, it.attestationBlob.idFormat)
                        val parsedMetadata = JSONObject(it.attestationBlob.metadata!!)

                        val map = mapOf(
                            "presentation" to "attestation",
                            "metadata" to it.attestationBlob.metadata,
                            "attestationHash" to attestation.getHash().toHex(),
                            "signature" to it.attestationBlob.signature!!.toHex(),
                            "signee_key" to IPv8Android.getInstance().myPeer.publicKey.keyToBin().toHex(),
                            "attestor_key" to it.attestationBlob.attestorKey!!.keyToBin().toHex()
                        )

                        QRCodeDialog("Attestation for ${parsedMetadata.optString("attribute", "UNKNOWN")}", "${parsedMetadata.optString("attribute", "UNKNOWN")}: ${parsedMetadata.optString("value", "UNKNOWN")}", mapToJSON(map).toString())
                            .show(parentFragmentManager, tag)
                    }
                    Log.d("TESTJE", "CLICK attestation ${it.attestationBlob}")
                }
            ) {
                Log.d("TESTJE", "LONG CLICK attestation ${it.attestationBlob}")
                ConfirmDialog("Are you sure to delete this attestation?") { dialog ->
                    try {
                        attestationCommunity().database.deleteAttestationByHash(it.attestationBlob.attestationHash)
                        updateAttestations()
                    }catch(e: Exception) {
                        Toast.makeText(requireContext(), "Attestation couldn't be deleted", Toast.LENGTH_SHORT).show()
                    } finally {
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "Attestation succesfully deleted", Toast.LENGTH_SHORT).show()
                    }
                }
                    .show(parentFragmentManager, tag)
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onResume()

        binding.rvIdentities.adapter = adapterIdentity
        binding.rvIdentities.layoutManager = LinearLayoutManager(context)

        binding.rvAttributes.adapter = adapterAttributes
        binding.rvAttributes.layoutManager = LinearLayoutManager(context)

        binding.rvAttestations.adapter = adapterAttestations
        binding.rvAttestations.layoutManager = LinearLayoutManager(context)

        itemsIdentity.observe(
            viewLifecycleOwner,
            Observer {
                adapterIdentity.updateItems(it)
                toggleVisibility()
            }
        )

        lifecycleScope.launchWhenStarted {
            while (isActive) {
                updateAttestations()
                delay(1500)
            }
        }

        itemsAttributes.observe(
            viewLifecycleOwner,
            Observer {
                adapterAttributes.updateItems(it)
                toggleVisibility()
            }
        )

        binding.ivAddAttribute.setOnClickListener {
            addAttribute()
        }

        binding.ivAddAttestation.setOnClickListener {
            addAttestation()
        }
    }

    private fun toggleVisibility() {
        binding.tvNoAttestations.isVisible = adapterAttestations.itemCount == 0
        binding.tvNoAttributes.isVisible = adapterAttributes.itemCount == 0
        binding.ivAddAttribute.isVisible = getUnusedAttributeNames().isNotEmpty()
    }

    private fun updateAttestations() {
        val oldCount = adapterAttestations.itemCount
        val itemsAttestations = attestationCommunity().database.getAllAttestations()

        if(oldCount != itemsAttestations.size) {
            adapterAttestations.updateItems(
                createAttestationItems(itemsAttestations)
            )
        }

        toggleVisibility()
    }

    private fun addAttribute() {
        IdentityAttributeAddDialog(null, getUnusedAttributeNames(), identityCommunity()).show(parentFragmentManager, tag)
    }

    private fun addAttestation() {
        Toast.makeText(requireContext(), "Add attestation (TODO)", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.identity_options, menu)

        menu.getItem(0).isVisible = !identityStore.hasIdentity()
        menu.getItem(1).isVisible = identityStore.hasIdentity()
        menu.getItem(2).isVisible = identityStore.hasIdentity()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.actionAddIdentity -> {
                IdentityDetailsDialog(null, identityCommunity()).show(parentFragmentManager, tag)
            }
            R.id.actionEditIdentity -> {
                IdentityDetailsDialog(identityStore.getIdentity(), identityCommunity()).show(parentFragmentManager, tag)
            }
            R.id.actionRemoveIdentity -> {
                ConfirmDialog("Are you sure to delete your identity?") { dialog ->
                    try {
                        identityStore.deleteAllAttributes()

                        attestationCommunity().database.getAllAttestations().forEach {
                            attestationCommunity().database.deleteAttestationByHash(it.attestationHash)
                        }

                        val identity = identityStore.getIdentity()
                        if(identity != null) {
                            identityStore.deleteIdentity(identity)
                        }
                    }catch(e: Exception) {
                        Toast.makeText(requireContext(), "Identity couldn't be deleted", Toast.LENGTH_SHORT).show()
                    } finally {
                        dialog.dismiss()
//                        requireActivity().invalidateOptionsMenu()

//                        (requireActivity() as ValueTransferMainActivity).switchToFragment(ValueTransferMainActivity.walletOverviewFragmentTag)
//                        (requireActivity() as ValueTransferMainActivity).selectBottomNavigationItem(ValueTransferMainActivity.walletOverviewFragmentTag)

                        Toast.makeText(requireContext(), "Identity successfully deleted. Application re-initialized.", Toast.LENGTH_SHORT).show()
//                        (requireActivity() as ValueTransferMainActivity).recreate()
                        (requireActivity() as ValueTransferMainActivity).reloadActivity()


//                        findNavController().popBackStack(this.id, false)
//                        findNavController().navigate(nl.tudelft.trustchain.valuetransfer.R.id.action_identityFragment_to_walletOverViewFragment)
                    }
                }
                    .show(parentFragmentManager, tag)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getUnusedAttributeNames(): List<String> {
        val attributes = resources.getStringArray(R.array.identity_attributes)
        val currentAttributeNames = identityStore.getAttributeNames()

        return attributes.filter { name ->
            !currentAttributeNames.contains(name)
        }
    }

    private fun createAttributeItems(attributes: List<Attribute>): List<Item> {
        return attributes.map { attribute ->
            AttributeItem(attribute)
        }
    }

    private fun createAttestationItems(attestations: List<AttestationBlob>): List<Item> {
        return attestations
            .mapIndexed { index, blob ->
                DatabaseItem(index, blob)
            }
            .sortedBy {
                if (it.attestationBlob.metadata != null) {
                    return@sortedBy JSONObject(it.attestationBlob.metadata!!).optString("attribute")
                } else {
                    return@sortedBy ""
                }
            }
    }

    private fun createIdentityItems(identities: List<Identity>): List<Item> {
        return identities.map { identity ->
            IdentityItem(
                identity
            )
        }
    }
}
