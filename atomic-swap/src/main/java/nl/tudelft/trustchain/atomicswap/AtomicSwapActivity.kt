package nl.tudelft.trustchain.atomicswap

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.BaseActivity
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_peers.*
import nl.tudelft.trustchain.atomicswap.ui.peers.AddressItemRenderer
import nl.tudelft.trustchain.atomicswap.ui.peers.PeerItemRenderer
import kotlinx.coroutines.isActive
import nl.tudelft.trustchain.atomicswap.ui.peers.AddressItem
import nl.tudelft.trustchain.atomicswap.ui.peers.PeerItem
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import kotlinx.coroutines.delay


@RequiresApi(Build.VERSION_CODES.M)
class AtomicSwapActivity : BaseActivity() {
    override val navigationGraph get() = R.navigation.atomic_swap_navigation_graph
    override val bottomNavigationMenu get() = R.menu.atomic_swap_menu

    private val adapter = ItemAdapter()


    private val BLUETOOTH_PERMISSIONS_REQUEST_CODE = 200
    private val SETTINGS_INTENT_CODE = 1000

    private val BLUETOOTH_PERMISSIONS_SCAN = "android.permission.BLUETOOTH_SCAN"
    private val BLUETOOTH_PERMISSIONS_CONNECT = "android.permission.BLUETOOTH_CONNECT"
    private val BLUETOOTH_PERMISSIONS_ADVERTISE = "android.permission.BLUETOOTH_ADVERTISE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle init of IPv8 after requesting permissions; only if Android 12 or higher.
        // onPermissionsDenied() is run until user has accepted permissions.
        val BUILD_VERSION_CODE_S = 31
        if (Build.VERSION.SDK_INT >= BUILD_VERSION_CODE_S) {
            if (!hasBluetoothPermissions()) {
                requestBluetoothPermissions()
            } else {
                // Only initialize IPv8 if it has not been initialized yet.
                try {
                    IPv8Android.getInstance()
                } catch (exception: Exception) {
                    (application as AtomicSwapApplication).initIPv8()
                }
            }
        }
//
//        setContentView(R.layout.fragment_peers)

        adapter.registerRenderer(PeerItemRenderer {
            // NOOP
        })

        adapter.registerRenderer(AddressItemRenderer {
            // NOOP
        })

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))

        loadNetworkInfo()
        //receiveGossips()
    }

    private fun loadNetworkInfo() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val demoCommunity = IPv8Android.getInstance().getOverlay<AtomicSwapCommunity>()!!
                val peers = demoCommunity.getPeers()

                val discoveredAddresses = demoCommunity.network
                    .getWalkableAddresses(demoCommunity.serviceId)

                val discoveredBluetoothAddresses = demoCommunity.network
                    .getNewBluetoothPeerCandidates()
                    .map { it.address }

                val peerItems = peers.map {
                    PeerItem(
                        it
                    )
                }

                val addressItems = discoveredAddresses.map { address ->
                    val contacted = demoCommunity.discoveredAddressesContacted[address]
                    AddressItem(
                        address,
                        null,
                        contacted
                    )
                }

                val bluetoothAddressItems = discoveredBluetoothAddresses.map { address ->
                    AddressItem(
                        address,
                        null,
                        null
                    )
                }

                val items = peerItems + bluetoothAddressItems + addressItems

                for (peer in peers) {
                    Log.d("DemoCommunity", "FOUND PEER with id " + peer.mid)
                }


                adapter.updateItems(items)
                txtCommunityName.text = demoCommunity.javaClass.simpleName
                txtPeerCount.text = "${peers.size} peers"
                val textColorResId = if (peers.isNotEmpty()) R.color.green else R.color.red
                val textColor = ResourcesCompat.getColor(resources, textColorResId, null)
                txtPeerCount.setTextColor(textColor)
                imgEmpty.isVisible = items.isEmpty()
                demoCommunity.broadcastTradeOffer(1, 0.5)

                delay(3000)
            }
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return checkSelfPermission(BLUETOOTH_PERMISSIONS_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(BLUETOOTH_PERMISSIONS_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(BLUETOOTH_PERMISSIONS_SCAN) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBluetoothPermissions() {
        requestPermissions(
            arrayOf(
                BLUETOOTH_PERMISSIONS_ADVERTISE,
                BLUETOOTH_PERMISSIONS_CONNECT,
                BLUETOOTH_PERMISSIONS_SCAN
            ),
            BLUETOOTH_PERMISSIONS_REQUEST_CODE
        )
    }

}
