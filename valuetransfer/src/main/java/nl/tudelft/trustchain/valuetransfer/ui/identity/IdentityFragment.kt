package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
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
        store.getAllPersonalIdentities().map { identities ->
            createIdentityItems(identities)
        }.asLiveData()
    }

    private val itemsAttributes: LiveData<List<Item>> by lazy {
        store.getAllAttributes().map { attributes ->
            createAttributeItems(attributes)
        }.asLiveData()
    }

    private val store by lazy {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (activity as ValueTransferMainActivity).toggleActionBar(false)

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

        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bnvIdentity)
        val navController = requireActivity().findNavController(R.id.navHostFragment)
        bottomNavigationView.setupWithNavController(navController)

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

        binding.tvNoIdentity.setOnClickListener {
            IdentityDetailsDialog(null, identityCommunity()).show(parentFragmentManager, tag)
        }

        binding.tvNoAttributes.setOnClickListener {
            IdentityAttributeAddDialog(null, getUnusedAttributeNames(), identityCommunity()).show(parentFragmentManager, tag)
        }

        binding.tvNoAttestations.setOnClickListener {
            Log.d("TESTJE", "ADD ATTESTATION")
        }

        binding.rvIdentities.adapter = adapterIdentity
        binding.rvIdentities.layoutManager = LinearLayoutManager(context)

        binding.rvAttributes.adapter = adapterAttributes
        binding.rvAttributes.layoutManager = LinearLayoutManager(context)

        binding.rvAttestations.adapter = adapterAttestations
        binding.rvAttestations.layoutManager = LinearLayoutManager(context)
    }

    private fun toggleVisibility() {
        if(store.hasPersonalIdentity()) {
            binding.tvNoIdentity.visibility = View.GONE
            binding.tvAttributesTitle.visibility = View.VISIBLE
            binding.tvAttestationsTitle.visibility = View.VISIBLE

            if(adapterAttestations.itemCount == 0) {
                binding.tvNoAttestations.visibility = View.VISIBLE
            }else{
                binding.tvNoAttestations.visibility = View.GONE
            }

            if(adapterAttributes.itemCount == 0) {
                binding.tvNoAttributes.visibility = View.VISIBLE
            }else{
                binding.tvNoAttributes.visibility = View.GONE
            }
        }else{
            binding.tvNoIdentity.visibility = View.VISIBLE
            binding.tvNoAttributes.visibility = View.GONE
            binding.tvNoAttestations.visibility = View.GONE
            binding.tvAttributesTitle.visibility = View.GONE
            binding.tvAttestationsTitle.visibility = View.GONE
        }
    }

    private fun updateAttestations() {
        val oldCount = adapterAttestations.itemCount
        val itemsAttestations = attestationCommunity().database.getAllAttestations()

        toggleVisibility()

        if(oldCount != itemsAttestations.size) {
            adapterAttestations.updateItems(
                createAttestationItems(itemsAttestations)
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.identity_options, menu)

        menu.getItem(0).isVisible = !store.hasPersonalIdentity()
        menu.getItem(1).isVisible = store.hasPersonalIdentity()
        menu.getItem(2).isVisible = store.hasPersonalIdentity()
        menu.getItem(3).isVisible = store.hasPersonalIdentity()
        menu.getItem(4).isVisible = store.hasPersonalIdentity() && getUnusedAttributeNames().isNotEmpty()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.actionAddPersonalIdentity -> {
                IdentityDetailsDialog(null, identityCommunity()).show(parentFragmentManager, tag)
            }
            R.id.actionEditPersonalIdentity -> {
                IdentityDetailsDialog(store.getPersonalIdentity(), identityCommunity()).show(parentFragmentManager, tag)
            }
            R.id.actionRemovePersonalIdentity -> {
                ConfirmDialog("Are you sure to delete your identity?") { dialog ->
                    try {
                        store.deleteIdentity(store.getPersonalIdentity())
                    }catch(e: Exception) {
                        Toast.makeText(requireContext(), "Identity couldn't be deleted", Toast.LENGTH_SHORT).show()
                    } finally {
                        dialog.dismiss()
                        activity?.invalidateOptionsMenu()
                        Toast.makeText(requireContext(), "Identity succesfully deleted", Toast.LENGTH_SHORT).show()
                    }
                }
                    .show(parentFragmentManager, tag)
            }
            R.id.actionAddAttestation -> {
                Toast.makeText(requireContext(), "Add attestation", Toast.LENGTH_SHORT).show()
            }
            R.id.actionAddAttribute -> {
                IdentityAttributeAddDialog(null, getUnusedAttributeNames(), identityCommunity()).show(parentFragmentManager, tag)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getUnusedAttributeNames(): List<String> {
        val attributes = resources.getStringArray(R.array.identity_attributes)
        val currentAttributeNames = store.getAttributeNames()

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
