package nl.tudelft.trustchain.valuetransfer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.LinearLayout
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
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentIdentityBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.dialogs.IdentityDetailsDialog
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.IdentityItem
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.IdentityItemRenderer
import org.json.JSONObject
import java.util.*


class IdentityFragment : BaseFragment(R.layout.fragment_identity) {

    private val binding by viewBinding(FragmentIdentityBinding::bind)
    private val adapterPersonal = ItemAdapter()

    private val itemsPersonal: LiveData<List<Item>> by lazy {
        store.getAllPersonalIdentities().map { identities ->
            createItems(identities)
        }.asLiveData()
    }

    private val store by lazy {
        IdentityStore.getInstance(requireContext())
    }

    private fun getCommunity(): IdentityCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("IdentityCommunity is not configured")
    }

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapterPersonal.registerRenderer(
            IdentityItemRenderer(
                1,
                {
                    Log.d("LONG", "CLICK")

//                    showOptions(identity)
                }, { identity ->
                    Log.d(
                        "CLICKED",
                        "QR Code for personal item " + identity.publicKey.keyToBin().toHex()
                    )
                    dialogQRCode(
                        "Personal Public Key",
                        "Show QR-code to other party",
                        identity.publicKey.keyToBin().toHex()
                    )
                }, { identity ->
                    addToClipboard(identity.publicKey.keyToBin().toHex(), "Public Key")
                    Toast.makeText(
                        this.context,
                        "Public key copied to clipboard",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(
                        "CLICKED",
                        "Copy Public Key for personal item " + identity.publicKey.keyToBin().toHex()
                    )
                }
            )
        )

        itemsPersonal.observe(
            this,
            Observer {
                adapterPersonal.updateItems(it)

                if (store.hasPersonalIdentity()) {
                    binding.tvNoPersonalIdentity.visibility = View.GONE
//                    binding.btnAddPersonalIdentity.setBackgroundResource(R.drawable.ic_baseline_more_vert_24)
                } else {
                    binding.tvNoPersonalIdentity.visibility = View.VISIBLE
//                    binding.btnAddPersonalIdentity.setBackgroundResource(R.drawable.ic_baseline_add_24)
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding.btnAddPersonalIdentity.setOnClickListener {
//            when(store.hasPersonalIdentity()) {
//                true -> showPopupMenu(binding.btnAddPersonalIdentity)
//                false -> {
//                    Toast.makeText(this.context, "Add personal identity clicked", Toast.LENGTH_SHORT).show()
//                    IdentityDetailsDialog(null, getCommunity()).show(
//                        parentFragmentManager,
//                        tag
//                    )
//                }
//            }
//        }

        binding.rvPersonalIdentities.adapter = adapterPersonal
        binding.rvPersonalIdentities.layoutManager = LinearLayoutManager(context)
        binding.rvPersonalIdentities.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.identity_options, menu)

        menu.getItem(0).isVisible = !store.hasPersonalIdentity()
        menu.getItem(1).isVisible = store.hasPersonalIdentity()
        menu.getItem(2).isVisible = store.hasPersonalIdentity()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.actionAddPersonalIdentity -> IdentityDetailsDialog(null, getCommunity()).show(parentFragmentManager, tag)
            R.id.actionEditPersonalIdentity -> {
                Toast.makeText(this.context, "EDIT", Toast.LENGTH_SHORT).show()
                IdentityDetailsDialog(store.getPersonalIdentity(), getCommunity()).show(parentFragmentManager, tag)
            }
            R.id.actionRemovePersonalIdentity -> {
                Toast.makeText(this.context, "REMOVE", Toast.LENGTH_SHORT).show()
                store.deleteIdentity(store.getPersonalIdentity())
                activity?.invalidateOptionsMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }

//    private fun showPopupMenu(optionsMenuButton: Button) {
//        val optionsMenu = PopupMenu(this.context, optionsMenuButton)
//        optionsMenu.menuInflater.inflate(R.menu.personal_identity_options, optionsMenu.menu)
//
//        optionsMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
//            when (item.itemId) {
//                R.id.actionEditPersonalIdentity ->
//                    Toast.makeText(this.context, "Clicked edit personal identity", Toast.LENGTH_SHORT).show()
//                R.id.actionRemovePersonalIdentity ->
//                    Toast.makeText(this.context, "Clicked remove personal identity", Toast.LENGTH_SHORT).show()
//            }
//            true
//        })
//        optionsMenu.show()
//    }

//    private fun showOptions(identity: Identity) {
//        val actions = arrayOf("Edit", "Remove")
//        AlertDialog.Builder(requireContext())
//            .setItems(actions) { _, action ->
//                when (action) {
//                    0 -> {
//                        IdentityDetailsDialog(identity, getCommunity()).show(parentFragmentManager, tag)
//                    }
//                    1 -> {
//                        store.deleteIdentity(identity)
//                        activity?.invalidateOptionsMenu();
//                        Toast.makeText(
//                            this.context,
//                            "Removed identity: ${identity.publicKey}",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            }
//            .show()
//    }

    private fun createBitmap(attributes: Map<String, String>): Bitmap {
        val data = JSONObject()
        for ((key, value) in attributes) {
            data.put(key, value)
        }

        return qrCodeUtils.createQR(data.toString())!!
    }

    private fun dialogQRCode(title: String, subtitle: String, publicKey: String) {

        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_qrcode, null)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvSubTitle = view.findViewById<TextView>(R.id.tvSubTitle)
        val ivQRCode = view.findViewById<ImageView>(R.id.ivQRCode)
        val btnCloseDialog = view.findViewById<Button>(R.id.btnCloseDialog)

        builder.setView(view)
        val dialog: AlertDialog = builder.create()

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
            }, 100
        )

    }

    private fun addToClipboard(text: String, label: String) {
        val clipboard =
            ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }

    private fun createItem(identity: Identity): Item {
        return IdentityItem(identity)
    }

    private fun createItems(identities: List<Identity>): List<Item> {
        return identities.mapIndexed { _, identity ->
            IdentityItem(
                identity
            )
        }
    }
}
