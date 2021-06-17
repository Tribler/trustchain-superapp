package nl.tudelft.trustchain.valuetransfer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.flow.map
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentIdentityBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.IdentityItem
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.IdentityItemRenderer
import org.json.JSONObject

class IdentityFragment : BaseFragment(R.layout.fragment_identity) {

    private val binding by viewBinding(FragmentIdentityBinding::bind)
    private val adapterPersonal = ItemAdapter()
    private val adapterBusiness = ItemAdapter()

    private val itemsPersonal: LiveData<List<Item>> by lazy {
        store.getAllPersonalIdentities().map { identities ->
            createItems(identities)
        }.asLiveData()
    }

    private val itemsBusiness: LiveData<List<Item>> by lazy {
        store.getAllBusinessIdentities().map { identities ->
            createItems(identities)
        }.asLiveData()
    }

    private val store by lazy {
        IdentityStore.getInstance(requireContext())
    }

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapterPersonal.registerRenderer(
            IdentityItemRenderer(
                1,
                {
                    Log.d("CLICKED", it.publicKey.keyToBin().toHex())
                }, {
                    Log.d("LONG", "CLICK")
                }, {
                    Log.d("CLICKED", "QR Code for personal item "+ it.publicKey.keyToBin().toHex())
                    qrCodeDialog("Personal Public Key", "Show QR-code to other party", it.publicKey.keyToBin().toHex())
                }, {
                    addToClipboard(it.publicKey.keyToBin().toHex(), "Public Key")
                    Toast.makeText(this.context,"Public key copied to clipboard",Toast.LENGTH_SHORT).show()
                    Log.d("CLICKED", "Copy Public Key for personal item "+ it.publicKey.keyToBin().toHex())
                }
            )
        )

        itemsPersonal.observe(
            this,
            Observer {
                val oldCount = adapterPersonal.itemCount
                adapterPersonal.updateItems(it)
                if (adapterPersonal.itemCount != oldCount) {
                    binding.rvPersonalIdentities.scrollToPosition(adapterPersonal.itemCount - 1)
                }
            }
        )

        adapterBusiness.registerRenderer(
            IdentityItemRenderer(
            2,
                {
                    Log.d("CLICKED", it.publicKey.keyToBin().toHex())
                }, {
                   Log.d("LONG", "CLICK")
                }, {
                    qrCodeDialog("Business Public Key", "Show QR-code to other party", it.publicKey.keyToBin().toHex())
                    Log.d("CLICKED", "QR Code for business item "+ it.publicKey.keyToBin().toHex())
                }, {
                    addToClipboard(it.publicKey.keyToBin().toHex(), "Public Key")
                    Toast.makeText(this.context,"Public key copied to clipboard",Toast.LENGTH_SHORT).show()
                    Log.d("CLICKED", "Copy Public Key for personal item "+ it.publicKey.keyToBin().toHex())
                }
            )
        )

        itemsBusiness.observe(
            this,
            Observer {
                val oldCount = adapterBusiness.itemCount
                adapterBusiness.updateItems(it)
                if (adapterBusiness.itemCount != oldCount) {
                    binding.rvBusinessIdentities.scrollToPosition(adapterBusiness.itemCount - 1)
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(store.hasPersonalIdentity()) {
            binding.tvNoPersonalIdentity.visibility = View.GONE
        }

        val personalIdentityOptionsMenuButton = binding.btnOptionsPersonalIdentity
        personalIdentityOptionsMenuButton.setOnClickListener {
            val personalIdentityOptionsMenu = PopupMenu(this.context, personalIdentityOptionsMenuButton)
            personalIdentityOptionsMenu.menuInflater.inflate(R.menu.personal_identity_options, personalIdentityOptionsMenu.menu)
            personalIdentityOptionsMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.actionAddPersonalIdentity ->
                        Toast.makeText(this.context, "Clicked add personal identity", Toast.LENGTH_SHORT).show()
                    R.id.actionEditPersonalIdentity ->
                        Toast.makeText(this.context, "Clicked edit personal identity", Toast.LENGTH_SHORT).show()
                    R.id.actionRemovePersonalIdentity ->
                        Toast.makeText(this.context, "Clicked remove personal identity", Toast.LENGTH_SHORT).show()
                }
                true
            })
            personalIdentityOptionsMenu.show()
        }

        val businessIdentityOptionsMenuButton = binding.btnOptionsBusinessIdentity
        businessIdentityOptionsMenuButton.setOnClickListener {
            val businessIdentityOptionsMenu = PopupMenu(this.context, businessIdentityOptionsMenuButton)
            businessIdentityOptionsMenu.menuInflater.inflate(R.menu.business_identity_options, businessIdentityOptionsMenu.menu)
            businessIdentityOptionsMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.actionAddBusinessIdentity ->
                        Toast.makeText(this.context, "Clicked add business identity", Toast.LENGTH_SHORT).show()
                }
                true
            })
            businessIdentityOptionsMenu.show()
        }

        binding.rvPersonalIdentities.adapter = adapterPersonal
        binding.rvPersonalIdentities.layoutManager = LinearLayoutManager(context)
        binding.rvPersonalIdentities.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        binding.rvBusinessIdentities.adapter = adapterBusiness
        binding.rvBusinessIdentities.layoutManager = LinearLayoutManager(context)
        binding.rvBusinessIdentities.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )
    }

    private fun createBitmap(attributes: Map<String,String>): Bitmap {
        val data = JSONObject()
        for((key, value) in attributes) {
            data.put(key, value)
        }

        return qrCodeUtils.createQR(data.toString())!!
    }

    private fun qrCodeDialog(title: String, subtitle: String, publicKey: String) {

        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_qrcode, null)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvSubTitle = view.findViewById<TextView>(R.id.tvSubTitle)
        val ivQRCode = view.findViewById<ImageView>(R.id.ivQRCode)
        val btnCloseDialog = view.findViewById<Button>(R.id.btnCloseDialog)

        builder.setView(view)
        val dialog : AlertDialog = builder.create()

        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        Handler().postDelayed(
            Runnable {
                view.findViewById<ProgressBar>(R.id.pbLoadingSpinner).visibility = View.GONE
                tvTitle.text = title
                tvSubTitle.text = subtitle
                btnCloseDialog.visibility = View.VISIBLE

                val map = mapOf(
                    "public_key" to publicKey,
                    "message" to "TEST"
                )

                ivQRCode.setImageBitmap(createBitmap(map))
            }, 100)

    }

    private fun addToClipboard(text: String, label: String) {
        val clipboard = ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }

    private fun createItems(identities: List<Identity>): List<Item> {
        return identities.mapIndexed { _, identity ->
            IdentityItem(
                identity
            )
        }
    }
}
