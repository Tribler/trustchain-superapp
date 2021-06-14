package nl.tudelft.trustchain.ssi.ui.requests

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_requests.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.attestation.common.consts.SchemaConstants.ID_METADATA
import nl.tudelft.ipv8.attestation.wallet.consts.Metadata.ID_FORMAT
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.databinding.FragmentRequestsBinding
import nl.tudelft.trustchain.ssi.ui.dialogs.attestation.AttestationValueDialog
import nl.tudelft.trustchain.ssi.ui.requests.RequestItem.Companion.ATTESTATION_REQUEST_ITEM
import nl.tudelft.trustchain.ssi.ui.requests.RequestItem.Companion.VERIFY_REQUEST_ITEM
import org.json.JSONObject

class RequestsFragment : BaseFragment(R.layout.fragment_requests) {

    private val adapter = ItemAdapter()
    private val binding by viewBinding(FragmentRequestsBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter.registerRenderer(
            RequestRenderer(
                {
                    onPositiveClick(it)
                },
                {
                    onNegativeClick(it)
                }
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerViewRequests.adapter = adapter
        binding.recyclerViewRequests.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewRequests.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )
        binding.refreshLayout.setOnRefreshListener {
            loadRequestEntries()
        }
        this.loadRequestEntriesOnLoop()
    }

    private fun onPositiveClick(item: RequestItem) {
        val channel = Communication.load()
        if (item.isVerifyRequest()) {
            Log.d("ig-ssi", "Allowing verification from ${item.peer.mid} for ${item.attributeName}")
            channel.allowVerification(item.peer, item.attributeName)
        } else if (item.isAttestationRequest()) {
            val parsedMetadata = JSONObject(item.metadata!!)
            val idFormat = parsedMetadata.optString(ID_FORMAT, ID_METADATA)

            // val activity = weakActivity!!.get()
            fun callBack(value: String) {
                Log.i("ig-ssi", "Signing attestation.")
                channel.attest(item.peer, item.attributeName, value.toByteArray(Charsets.US_ASCII))
            }
            AttestationValueDialog(
                item.attributeName,
                idFormat,
                item.requestedValue,
                callback = ::callBack
            ).show(
                parentFragmentManager,
                "ig-ssi"
            )
        }
    }

    private fun onNegativeClick(item: RequestItem) {
        val channel = Communication.load()
        if (item.isVerifyRequest()) {
            Log.d(
                "ig-ssi",
                "Disallowing verification from ${item.peer.mid} for ${item.attributeName}"
            )
            channel.disallowVerification(item.peer, item.attributeName)
        } else if (item.isAttestationRequest()) {
            channel.dismissAttestationRequest(item.peer, item.attributeName)
        }
    }

    private fun loadRequestEntriesOnLoop() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                loadRequestEntries()
                delay(100)
            }
        }
    }

    private fun loadRequestEntries() {
        val channel =
            Communication.load()

        val attestationRequests = channel.attestationRequests
        val verificationRequests = channel.verifyRequests

        val items = mutableListOf<RequestItem>()
        attestationRequests.keys.forEachIndexed { index, attributePointer ->
            val request = attestationRequests[attributePointer]!!
            items.add(
                RequestItem(
                    index,
                    ATTESTATION_REQUEST_ITEM,
                    attributePointer.peer,
                    attributePointer.attributeName,
                    request.second,
                    request.third
                )
            )
        }

        verificationRequests.keys.forEachIndexed { index, attributePointer ->
            items.add(
                RequestItem(
                    index,
                    VERIFY_REQUEST_ITEM,
                    attributePointer.peer,
                    attributePointer.attributeName
                )
            )
        }

        adapter.updateItems(items)
        imgEmpty.isVisible = items.isEmpty()

        refreshLayout.isRefreshing = false
    }
}
