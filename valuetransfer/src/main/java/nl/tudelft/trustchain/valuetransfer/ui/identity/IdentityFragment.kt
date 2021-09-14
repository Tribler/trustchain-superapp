package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
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
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.valuetransfer.util.copyToClipboard
import nl.tudelft.trustchain.valuetransfer.util.mapToJSON
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentIdentityBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.dialogs.*
import nl.tudelft.trustchain.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import org.json.JSONObject
import java.util.*

class IdentityFragment : BaseFragment(R.layout.fragment_identity) {
    private val binding by viewBinding(FragmentIdentityBinding::bind)
    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var identityCommunity: IdentityCommunity
    private lateinit var attestationCommunity: AttestationCommunity
    private lateinit var identityStore: IdentityStore

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

    private var scanIntent: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_identity, container, false)
    }

    override fun onResume() {
        super.onResume()

        parentActivity.setActionBarTitle("Identity", null)
        parentActivity.toggleActionBar(false)
        parentActivity.toggleBottomNavigation(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        parentActivity = requireActivity() as ValueTransferMainActivity
        identityCommunity = parentActivity.getCommunity()!!
        attestationCommunity = parentActivity.getCommunity()!!
        identityStore = parentActivity.getStore()!!

        adapterIdentity.registerRenderer(
            IdentityItemRenderer(
                1,
                { identity ->
                    val map = mapOf(
                        "public_key" to identity.publicKey.keyToBin().toHex(),
                        "name" to identity.content.givenNames
                    )

                    QRCodeDialog("MY PUBLIC KEY", "Show the QR-code to the other party", mapToJSON(map).toString())
                        .show(parentFragmentManager, tag)
                },
                { identity ->
                    copyToClipboard(requireContext(), identity.publicKey.keyToBin().toHex(), "Public Key")
                    parentActivity.displaySnackbar(requireContext(), "Public key copied to clipboard")
                }
            )
        )

        adapterAttributes.registerRenderer(
            IdentityAttributeItemRenderer(
                { attribute ->
                    ConfirmDialog("Are you sure to delete this attribute?") { dialog ->
                        try {
                            identityStore.deleteAttribute(attribute)

                            activity?.invalidateOptionsMenu()
                            dialog.dismiss()

                            parentActivity.displaySnackbar(requireContext(), "Attribute succesfully deleted")
                        } catch (e: Exception) {
                            e.printStackTrace()
                            parentActivity.displaySnackbar(requireContext(), "Attribute couldn't be deleted", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                        }
                    }
                        .show(parentFragmentManager, tag)
                },
                { attribute ->
                    IdentityAttributeDialog(attribute).show(parentFragmentManager, tag)
                },
                { attribute ->
                    IdentityAttributeShareDialog(null, attribute).show(parentFragmentManager, tag)
                }
            )
        )

        adapterAttestations.registerRenderer(
            AttestationItemRenderer(
                {
                    val blob = it.attestationBlob

                    if (blob.signature != null) {
                        val manager = SchemaManager()
                        manager.registerDefaultSchemas()
                        val attestation = manager.deserialize(blob.blob, blob.idFormat)
                        val parsedMetadata = JSONObject(blob.metadata!!)

                        val map = mapOf(
                            "presentation" to "attestation",
                            "metadata" to blob.metadata,
                            "attestationHash" to attestation.getHash().toHex(),
                            "signature" to blob.signature!!.toHex(),
                            "signee_key" to IPv8Android.getInstance().myPeer.publicKey.keyToBin().toHex(),
                            "attestor_key" to blob.attestorKey!!.keyToBin().toHex()
                        )

                        QRCodeDialog("Attestation for ${parsedMetadata.optString("attribute", "UNKNOWN")}", "${parsedMetadata.optString("attribute", "UNKNOWN")}: ${parsedMetadata.optString("value", "UNKNOWN")}", mapToJSON(map).toString())
                            .show(parentFragmentManager, tag)
                    } else {
                        deleteAttestation(it)
                    }
                }
            ) {
                deleteAttestation(it)
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
                delay(1000)
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
            showPopup(binding.ivAddAttestation)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.identity_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionEditIdentity -> {
                IdentityDetailsDialog().show(parentFragmentManager, tag)
            }
            R.id.actionRemoveIdentity -> {
                ConfirmDialog("Are you sure to delete your identity?") {
                    try {
                        identityStore.deleteAllAttributes()

                        attestationCommunity.database.getAllAttestations().forEach {
                            attestationCommunity.database.deleteAttestationByHash(it.attestationHash)
                        }

                        val identity = identityStore.getIdentity()
                        if (identity != null) {
                            identityStore.deleteIdentity(identity)
                        }

                        parentActivity.reloadActivity()
                        parentActivity.displaySnackbar(requireContext(), "Identity successfully deleted. Application re-initialized.", isShort = false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        parentActivity.displaySnackbar(requireContext(), "Identity couldn't be deleted", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                    }
                }
                    .show(parentFragmentManager, tag)
            }
            R.id.actionViewAuthorities -> IdentityAttestationAuthoritiesDialog(
                trustchain.getMyPublicKey().toHex()
            ).show(parentFragmentManager, tag)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)?.let { result ->
            try {
                val obj = JSONObject(result)

                if (obj.has("public_key")) {
                    try {
                        defaultCryptoProvider.keyFromPublicBin(obj.optString("public_key").hexToBytes())
                        val publicKey = obj.optString("public_key")

                        when (scanIntent) {
                            ADD_ATTESTATION_INTENT -> parentActivity.getQRScanController().addAttestation(publicKey)
                            ADD_AUTHORITY_INTENT -> parentActivity.getQRScanController().addAuthority(publicKey)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        parentActivity.displaySnackbar(requireContext(), "Invalid public key in QR-code", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                    }
                } else {
                    parentActivity.displaySnackbar(requireContext(), "No public key found in QR-code", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                parentActivity.displaySnackbar(requireContext(), "Scanned QR code not in JSON format", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
            }
        }
    }

    private fun deleteAttestation(attestation: AttestationItem) {
        ConfirmDialog("Are you sure to delete this attestation?") { dialog ->
            try {
                attestationCommunity.database.deleteAttestationByHash(attestation.attestationBlob.attestationHash)
                updateAttestations()
            } catch (e: Exception) {
                e.printStackTrace()
                parentActivity.displaySnackbar(requireContext(), "Attestation couldn't be deleted", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
            } finally {
                dialog.dismiss()
                parentActivity.displaySnackbar(requireContext(), "Attestation succesfully deleted")
            }
        }
            .show(parentFragmentManager, tag)
    }

    private fun toggleVisibility() {
        binding.tvNoAttestations.isVisible = adapterAttestations.itemCount == 0
        binding.tvNoAttributes.isVisible = adapterAttributes.itemCount == 0
        binding.ivAddAttribute.isVisible = identityCommunity.getUnusedAttributeNames().isNotEmpty()
    }

    private fun updateAttestations() {
        val oldCount = adapterAttestations.itemCount
        val itemsAttestations = attestationCommunity.database.getAllAttestations()

        if (oldCount != itemsAttestations.size) {
            adapterAttestations.updateItems(
                createAttestationItems(itemsAttestations)
            )

            binding.rvAttestations.setItemViewCacheSize(itemsAttestations.size)
        }

        toggleVisibility()
    }

    private fun addAttribute() {
        IdentityAttributeDialog(null).show(parentFragmentManager, tag)
    }

    private fun addAttestation() {
        scanIntent = ADD_ATTESTATION_INTENT
        QRCodeUtils(requireContext()).startQRScanner(this, promptText = "Scan public key of signee to add attestation", vertical = true)
    }

    private fun addAuthority() {
        scanIntent = ADD_AUTHORITY_INTENT
        QRCodeUtils(requireContext()).startQRScanner(this, promptText = "Scan public key of signee to add as authority", vertical = true)
    }

    private fun showPopup(view: View) {
        val popupMenu = PopupMenu(requireContext(), view, Gravity.END)
        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.identity_attestations_options, popupMenu.menu)
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.actionAddAttestation -> addAttestation()
                R.id.actionAddAuthority -> addAuthority()
            }
            true
        }
    }

    private fun createAttributeItems(attributes: List<IdentityAttribute>): List<Item> {
        return attributes.map { attribute ->
            IdentityAttributeItem(attribute)
        }
    }

    private fun createAttestationItems(attestations: List<AttestationBlob>): List<Item> {
        return attestations
            .map { blob ->
                AttestationItem(blob)
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

    companion object {
        private const val ADD_ATTESTATION_INTENT = 0
        private const val ADD_AUTHORITY_INTENT = 1
    }
}
