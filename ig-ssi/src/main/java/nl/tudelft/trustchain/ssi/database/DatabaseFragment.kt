package nl.tudelft.trustchain.ssi.database

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_database.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.schema.SchemaManager
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.ssi.PresentAttestationDialog
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.databinding.FragmentDatabaseBinding
import org.json.JSONObject

private val logger = KotlinLogging.logger {}

class DatabaseFragment : BaseFragment(R.layout.fragment_database) {

    private val adapter = ItemAdapter()
    private val binding by viewBinding(FragmentDatabaseBinding::bind)

    lateinit var bitmap: Bitmap

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tag = this.tag
        adapter.registerRenderer(
            DatabaseItemRenderer {
                val dialog = PresentAttestationDialog(it.attestationBlob.idFormat)
                dialog.show(
                    parentFragmentManager,
                    tag
                )
                lifecycleScope.launch {
                    val metadata = it.attestationBlob.metadata
                    val manager = SchemaManager()
                    manager.registerDefaultSchemas()
                    val attestation =
                        manager.deserialize(it.attestationBlob.blob, it.attestationBlob.idFormat)

                    val signature = it.attestationBlob.signature!!.toHex()
                    val attestorKey = it.attestationBlob.attestorKey!!.keyToBin().toHex()
                    val key = IPv8Android.getInstance().myPeer.publicKey.keyToBin().toHex()

                    val data = JSONObject()
                    data.put("presentation", "attestation")
                    data.put("metadata", metadata)
                    data.put("attestationHash", attestation.getHash().toHex())
                    data.put("signature", signature)
                    data.put("signee_key", key)
                    data.put("attestor_key", attestorKey)
                    logger.debug("SSI: Presenting Attestation as QRCode: Size ${data.toString().length}, Data: $data")
                    val bitmap = withContext(Dispatchers.Default) {
                        qrCodeUtils.createQR(data.toString(), 750)!!
                    }
                    dialog.setQRCode(bitmap)
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        if (!this::bitmap.isInitialized) {
            lifecycleScope.launch {
                val data = JSONObject()
                data.put("presentation", "authority")
                val publicKey = IPv8Android.getInstance().myPeer.publicKey.keyToBin().toHex()
                data.put("public_key", publicKey)

                bitmap = withContext(Dispatchers.Default) {
                    qrCodeUtils.createQR(data.toString())!!
                }
                try {
                    binding.qrCodePlaceHolder.visibility = View.GONE
                    binding.publicKeyQRCode.setImageBitmap(bitmap)
                } catch (e: IllegalStateException) {
                    // This happens if we already switched screens.
                }
            }
        } else {
            binding.publicKeyQRCode.setImageBitmap(bitmap)
        }

        binding.myPublicKey.text = getIpv8().myPeer.mid
        loadDatabaseEntries()

        binding.publicKeyQRCode.setOnClickListener {
            if (it.scaleX > 1) {
                it.animate().scaleX(1f).scaleY(1f)
            } else {
                it.animate().scaleX(1.2f).scaleY(1.2f)
            }
        }
    }

    private fun loadDatabaseEntries() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val attestationCommunity =
                    IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
                val entries = attestationCommunity.database.getAllAttestations()
                    .mapIndexed { index, blob -> DatabaseItem(index, blob) }

                adapter.updateItems(entries)
                databaseTitle.text = "database.db"
                txtAttestationCount.text = "${entries.size} entries"
                val textColorResId = if (entries.isNotEmpty()) R.color.green else R.color.red
                val textColor = ResourcesCompat.getColor(resources, textColorResId, null)
                txtAttestationCount.setTextColor(textColor)
                imgEmpty.isVisible = entries.isEmpty()

                delay(1000)
            }
        }
    }
}
