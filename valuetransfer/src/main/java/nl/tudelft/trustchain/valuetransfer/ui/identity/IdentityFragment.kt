package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.schema.SchemaManager
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.valuetransfer.util.copyToClipboard
import nl.tudelft.trustchain.valuetransfer.util.mapToJSON
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTFragment
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentIdentityBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.*
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.common.valuetransfer.extensions.decodeImage
import nl.tudelft.trustchain.common.valuetransfer.extensions.encodeImage
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.common.valuetransfer.extensions.exitEnterView
import nl.tudelft.trustchain.valuetransfer.util.DividerItemDecorator
import nl.tudelft.trustchain.valuetransfer.util.getInitials
import org.json.JSONObject
import java.util.*

class IdentityFragment : VTFragment(R.layout.fragment_identity) {
    private val binding by viewBinding(FragmentIdentityBinding::bind)

    private val adapterIdentity = ItemAdapter()
    private val adapterAttributes = ItemAdapter()
    private val adapterAttestations = ItemAdapter()

    private val identityImage = MutableLiveData<String?>()

    private val itemsIdentity: LiveData<List<Item>> by lazy {
        combine(getIdentityStore().getAllIdentities(), identityImage.asFlow()) { identities, identityImage ->
            createIdentityItems(identities, identityImage)
        }.asLiveData()
    }

    private val itemsAttributes: LiveData<List<Item>> by lazy {
        getIdentityStore().getAllAttributes().map { attributes ->
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

    override fun initView() {
        parentActivity.apply {
            setActionBarTitle(resources.getString(R.string.menu_navigation_identity), null)
            toggleActionBar(false)
            toggleBottomNavigation(true)
        }
    }

    init {
        setHasOptionsMenu(true)

        lifecycleScope.launchWhenCreated {
            while (isActive) {
                if (appPreferences.getIdentityFace() != identityImage.value) {
                    identityImage.postValue(appPreferences.getIdentityFace())
                    parentActivity.invalidateOptionsMenu()
                }

                delay(1000)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapterIdentity.registerRenderer(
            IdentityItemRenderer(
                1,
                { identity ->
                    val map = mapOf(
                        QRScanController.KEY_PUBLIC_KEY to identity.publicKey.keyToBin().toHex(),
                        QRScanController.KEY_NAME to identity.content.let {
                            "${it.givenNames.getInitials()} ${it.surname}"
                        },
                    )

                    QRCodeDialog(resources.getString(R.string.text_my_public_key), resources.getString(R.string.text_public_key_share_desc), mapToJSON(map).toString())
                        .show(parentFragmentManager, tag)
                },
                { identity ->
                    copyToClipboard(
                        requireContext(),
                        identity.publicKey.keyToBin().toHex(),
                        resources.getString(R.string.text_public_key)
                    )
                    parentActivity.displayToast(
                        requireContext(),
                        resources.getString(
                            R.string.snackbar_copied_clipboard,
                            resources.getString(R.string.text_public_key)
                        )
                    )
                },
                {
                    if (identityImage.value!!.isBlank()) {
                        identityImageIntent()
                    } else {
                        OptionsDialog(
                            R.menu.identity_image_options,
                            "Choose Option",
                            bigOptionsEnabled = true,
                            menuMods = { menu ->
                                menu.apply {
                                    findItem(R.id.actionDeleteIdentityImage).isVisible =
                                        identityImage.value!!.isNotBlank()
                                }
                            },
                            optionSelected = { _, item ->
                                when (item.itemId) {
                                    R.id.actionAddIdentityImage -> identityImageIntent()
                                    R.id.actionDeleteIdentityImage -> {
                                        appPreferences.deleteIdentityFace()
                                        parentActivity.invalidateOptionsMenu()
                                    }
                                }
                            }
                        ).show(parentFragmentManager, tag)
                    }
                }
            )
        )

        adapterAttributes.registerRenderer(
            IdentityAttributeItemRenderer(
                1
            ) {
                OptionsDialog(
                    R.menu.identity_attribute_options,
                    "Choose Option",
                    bigOptionsEnabled = true,
                ) { _, item ->
                    when (item.itemId) {
                        R.id.actionEditIdentityAttribute -> IdentityAttributeDialog(it).show(
                            parentFragmentManager,
                            tag
                        )
                        R.id.actionDeleteIdentityAttribute -> deleteIdentityAttribute(it)
                        R.id.actionShareIdentityAttribute -> IdentityAttributeShareDialog(
                            null,
                            it
                        ).show(parentFragmentManager, tag)
                    }
                }.show(parentFragmentManager, tag)
            }
        )

        adapterAttestations.registerRenderer(
            AttestationItemRenderer(
                parentActivity,
                {
                    val blob = it.attestationBlob

                    if (blob.signature != null) {
                        val manager = SchemaManager()
                        manager.registerDefaultSchemas()
                        val attestation = manager.deserialize(blob.blob, blob.idFormat)
                        val parsedMetadata = JSONObject(blob.metadata!!)

                        val map = mapOf(
                            QRScanController.KEY_PRESENTATION to QRScanController.VALUE_ATTESTATION,
                            QRScanController.KEY_METADATA to blob.metadata,
                            QRScanController.KEY_ATTESTATION_HASH to attestation.getHash().toHex(),
                            QRScanController.KEY_SIGNATURE to blob.signature!!.toHex(),
                            QRScanController.KEY_SIGNEE_KEY to IPv8Android.getInstance().myPeer.publicKey.keyToBin().toHex(),
                            QRScanController.KEY_ATTESTOR_KEY to blob.attestorKey!!.keyToBin().toHex()
                        )

                        QRCodeDialog(
                            resources.getString(R.string.dialog_title_attestation),
                            StringBuilder()
                                .append(
                                    parsedMetadata.optString(
                                        QRScanController.KEY_ATTRIBUTE,
                                        QRScanController.FALLBACK_UNKNOWN
                                    )
                                )
                                .append(
                                    parsedMetadata.optString(
                                        QRScanController.KEY_VALUE,
                                        QRScanController.FALLBACK_UNKNOWN
                                    )
                                )
                                .toString(),
                            mapToJSON(map).toString()
                        )
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

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()

        binding.rvIdentities.apply {
            adapter = adapterIdentity
            layoutManager = LinearLayoutManager(context)
        }

        binding.rvAttributes.apply {
            adapter = adapterAttributes
            layoutManager = LinearLayoutManager(context)
            val drawable = ResourcesCompat.getDrawable(resources, R.drawable.divider_identity_attribute, requireContext().theme)
            addItemDecoration(DividerItemDecorator(drawable!!) as RecyclerView.ItemDecoration)
        }

        binding.rvAttestations.apply {
            adapter = adapterAttestations
            layoutManager = LinearLayoutManager(context)
            val drawable = ResourcesCompat.getDrawable(resources, R.drawable.divider_attestation, requireContext().theme)
            addItemDecoration(DividerItemDecorator(drawable!!) as RecyclerView.ItemDecoration)
        }

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

        binding.ivAddAttributeAttestation.setOnClickListener {
            OptionsDialog(
                R.menu.identity_add_options,
                resources.getString(R.string.dialog_choose_option),
                menuMods = { menu ->
                    menu.apply {
                        findItem(R.id.actionAddIdentityAttribute).isVisible = getIdentityCommunity().getUnusedAttributeNames().isNotEmpty()
                    }
                },
                optionSelected = { _, item ->
                    when (item.itemId) {
                        R.id.actionAddIdentityAttribute -> addIdentityAttribute()
                        R.id.actionAddAttestation -> addAttestation()
                        R.id.actionAddAuthority -> addAuthority()
                    }
                }
            ).show(parentFragmentManager, tag)
        }

        binding.tvShowIdentityAttributes.setOnClickListener {
            if (binding.clIdentityAttributes.isVisible) return@setOnClickListener

            binding.tvShowIdentityAttributes.apply {
                setTypeface(null, Typeface.BOLD)
                background = ContextCompat.getDrawable(requireContext(), R.drawable.pill_rounded_selected)
            }
            binding.tvShowAttestations.apply {
                setTypeface(null, Typeface.NORMAL)
                background = ContextCompat.getDrawable(requireContext(), R.drawable.pill_rounded)
            }

            binding.clAttestations.exitEnterView(requireContext(), binding.clIdentityAttributes, false)
        }

        binding.tvShowAttestations.setOnClickListener {
            if (binding.clAttestations.isVisible) return@setOnClickListener

            binding.tvShowIdentityAttributes.apply {
                setTypeface(null, Typeface.NORMAL)
                background = ContextCompat.getDrawable(requireContext(), R.drawable.pill_rounded)
            }
            binding.tvShowAttestations.apply {
                setTypeface(null, Typeface.BOLD)
                background = ContextCompat.getDrawable(requireContext(), R.drawable.pill_rounded_selected)
            }
            binding.clIdentityAttributes.exitEnterView(requireContext(), binding.clAttestations, true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        menu.add(Menu.NONE, MENU_ITEM_OPTIONS, Menu.NONE, null)
            .setIcon(R.drawable.ic_baseline_more_vert_24)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
    }

    @SuppressLint("RestrictedApi")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        OptionsDialog(
            R.menu.identity_options,
            resources.getString(R.string.dialog_choose_option),
        ) { _, selectedItem ->
            when (selectedItem.itemId) {
                R.id.actionViewAuthorities -> IdentityAttestationAuthoritiesDialog(
                    trustchain.getMyPublicKey().toHex()
                ).show(parentFragmentManager, tag)
            }
        }.show(parentFragmentManager, tag)

        return super.onOptionsItemSelected(item)
    }

    private fun toggleVisibility() {
        binding.tvNoAttestations.isVisible = adapterAttestations.itemCount == 0
        binding.tvNoAttributes.isVisible = adapterAttributes.itemCount == 0
    }

    private fun addAttestation() {
        scanIntent = ADD_ATTESTATION_INTENT
        QRCodeUtils(requireContext()).startQRScanner(
            this,
            promptText = resources.getString(R.string.text_scan_public_key_to_add_attestation),
            vertical = true
        )
    }

    private fun updateAttestations() {
        val oldCount = adapterAttestations.itemCount
        val itemsAttestations = getAttestationCommunity().database.getAllAttestations()

        if (oldCount != itemsAttestations.size) {
            adapterAttestations.updateItems(
                createAttestationItems(itemsAttestations)
            )

            binding.rvAttestations.setItemViewCacheSize(itemsAttestations.size)
        }

        toggleVisibility()
    }

    private fun deleteAttestation(attestation: AttestationItem) {
        ConfirmDialog(
            resources.getString(
                R.string.text_confirm_delete,
                resources.getString(R.string.text_this_attestation)
            )
        ) { dialog ->
            try {
                getAttestationCommunity().database.deleteAttestationByHash(attestation.attestationBlob.attestationHash)
                updateAttestations()

                dialog.dismiss()
                parentActivity.displayToast(
                    requireContext(),
                    resources.getString(R.string.snackbar_attestation_remove_success)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                parentActivity.displayToast(
                    requireContext(),
                    resources.getString(R.string.snackbar_attestation_remove_error)
                )
            }
        }
            .show(parentFragmentManager, tag)
    }

    private fun addIdentityAttribute() {
        IdentityAttributeDialog(null).show(parentFragmentManager, tag)
    }

    private fun deleteIdentityAttribute(attribute: IdentityAttribute) {
        ConfirmDialog(
            resources.getString(
                R.string.text_confirm_delete,
                resources.getString(R.string.text_this_attribute)
            )
        ) { dialog ->
            try {
                getIdentityStore().deleteAttribute(attribute)

                activity?.invalidateOptionsMenu()
                dialog.dismiss()

                parentActivity.displayToast(
                    requireContext(),
                    resources.getString(R.string.snackbar_identity_attribute_remove_success)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                parentActivity.displayToast(
                    requireContext(),
                    resources.getString(R.string.snackbar_identity_attribute_remove_error)
                )
            }
        }
            .show(parentFragmentManager, tag)
    }

    private fun addAuthority() {
        scanIntent = ADD_AUTHORITY_INTENT
        QRCodeUtils(requireContext()).startQRScanner(
            this,
            promptText = resources.getString(R.string.text_scan_public_key_to_add_authority),
            vertical = true
        )
    }

    private fun identityImageIntent() {
        val mimeTypes = arrayOf("image/*")
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(
            Intent.createChooser(
                intent,
                resources.getString(R.string.text_send_photo_video)
            ),
            PICK_IDENTITY_IMAGE
        )
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
                    return@sortedBy JSONObject(it.attestationBlob.metadata!!).optString(QRScanController.KEY_ATTRIBUTE)
                } else {
                    return@sortedBy ""
                }
            }
    }

    private fun createIdentityItems(identities: List<Identity>, imageString: String?): List<Item> {
        return identities.map { identity ->
            IdentityItem(
                identity,
                imageString?.let { decodeImage(it) },
                false
            )
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IDENTITY_IMAGE) {
                if (data != null) {
                    data.data?.let { uri ->
                        val bitmap = if (Build.VERSION.SDK_INT >= 29) {
                            val source = ImageDecoder.createSource(parentActivity.contentResolver, uri)
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            MediaStore.Images.Media.getBitmap(parentActivity.contentResolver, uri)
                        }

                        appPreferences.setIdentityFace(encodeImage(bitmap))
                    }
                }
            } else {
                QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)
                    ?.let { result ->
                        try {
                            val obj = JSONObject(result)

                            if (obj.has(QRScanController.KEY_PUBLIC_KEY)) {
                                try {
                                    defaultCryptoProvider.keyFromPublicBin(
                                        obj.optString(
                                            QRScanController.KEY_PUBLIC_KEY
                                        ).hexToBytes()
                                    )
                                    val publicKey = obj.optString(QRScanController.KEY_PUBLIC_KEY)

                                    when (scanIntent) {
                                        ADD_ATTESTATION_INTENT -> getQRScanController().addAttestation(
                                            publicKey
                                        )
                                        ADD_AUTHORITY_INTENT -> getQRScanController().addAuthority(
                                            publicKey
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    parentActivity.displayToast(
                                        requireContext(),
                                        resources.getString(R.string.snackbar_invalid_public_key)
                                    )
                                }
                            } else {
                                parentActivity.displayToast(
                                    requireContext(),
                                    resources.getString(R.string.snackbar_no_public_key_found)
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            parentActivity.displayToast(
                                requireContext(),
                                resources.getString(R.string.snackbar_qr_code_not_json_format)
                            )
                        }
                    }
            }
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val ADD_ATTESTATION_INTENT = 0
        private const val ADD_AUTHORITY_INTENT = 1
        private const val MENU_ITEM_OPTIONS = 1234
        private const val PICK_IDENTITY_IMAGE = 2
    }
}
