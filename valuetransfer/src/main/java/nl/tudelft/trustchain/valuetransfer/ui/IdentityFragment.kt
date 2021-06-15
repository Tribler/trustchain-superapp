package nl.tudelft.trustchain.valuetransfer.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.flow.map
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentIdentityBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.IdentityItem
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.IdentityItemRenderer

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

    private val publicKeyBin by lazy {
        requireArguments().getString(ARG_PUBLIC_KEY)!!
    }

    private val publicKey by lazy {
        defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
    }

    private val name by lazy {
        requireArguments().getString(ARG_NAME)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapterPersonal.registerRenderer(IdentityItemRenderer(
            1
        ) {
            Log.d("CLICKED",it.publicKey.keyToBin().toHex())
        })

        Log.d("ITEMS", itemsPersonal.toString())

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

        adapterBusiness.registerRenderer(IdentityItemRenderer(
            2
        ) {
            Log.d("CLICKED",it.publicKey.keyToBin().toHex())
        })

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

    private fun createItems(identities: List<Identity>): List<Item> {
        return identities.mapIndexed { _, identity ->
            IdentityItem(
                identity
            )
        }
    }

    companion object {
        const val ARG_PUBLIC_KEY = "public_key"
        const val ARG_NAME = "name"

        private const val GROUP_TIME_LIMIT = 60 * 1000
        private const val PICK_IMAGE = 10
    }

}
