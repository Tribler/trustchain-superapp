package nl.tudelft.trustchain.eurotoken.ui.settings

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.trustchain.common.eurotoken.Gateway
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.FragmentGatewaysBinding
import nl.tudelft.trustchain.eurotoken.ui.EurotokenBaseFragment


class GatewaysFragment : EurotokenBaseFragment(R.layout.fragment_gateways) {
    private val binding by viewBinding(FragmentGatewaysBinding::bind)

    private val adapter = ItemAdapter()

    private val store by lazy {
        GatewayStore.getInstance(requireContext())
    }

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf<Item>()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(GatewayItemRenderer({
            showOptions(it)
        }))

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                val items = store.getGateways().map { gateway: Gateway -> GatewayItem(gateway) }
                adapter.updateItems(items)
                adapter.notifyDataSetChanged()
                delay(1000L)
            }
        }
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

        items.observe(viewLifecycleOwner, Observer {
            adapter.updateItems(it)
        })
    }

    private fun showOptions(gateway: Gateway) {
        val items = arrayOf("Make preferred", "Rename", "Delete")
        AlertDialog.Builder(requireContext())
            .setItems(items) { _, which ->
                when (which) {
                    0 -> makePreferred(gateway)
                    1 -> renameGateway(gateway)
                    2 -> deleteGateway(gateway)
                }
            }
            .show()
    }

    private fun makePreferred(gateway: Gateway) {
        store.setPreferred(gateway)
    }

    private fun renameGateway(gateway: Gateway) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Rename gateway")

        // Set up the input
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(gateway.name)
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton(
            "Rename"
        ) { _, _ ->
            store.updateGateway(gateway.publicKey, input.text.toString(), gateway.ip, gateway.port)
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun deleteGateway(gateway: Gateway) {
        store.deleteGateway(gateway)
    }
}
