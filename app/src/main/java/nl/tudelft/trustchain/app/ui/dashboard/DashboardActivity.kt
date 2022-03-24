package nl.tudelft.trustchain.app.ui.dashboard

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.app.AppDefinition
import nl.tudelft.trustchain.app.R
import nl.tudelft.trustchain.app.TrustChainApplication
import nl.tudelft.trustchain.app.databinding.ActivityDashboardBinding
import nl.tudelft.trustchain.common.ebsi.ConformanceTest
import nl.tudelft.trustchain.common.util.viewBinding
import java.util.*

@RequiresApi(Build.VERSION_CODES.M)
class DashboardActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityDashboardBinding::inflate)

    private val adapter = ItemAdapter()

    private val BLUETOOTH_PERMISSIONS_REQUEST_CODE = 200

    private val BLUETOOTH_PERMISSIONS_SCAN = "android.permission.BLUETOOTH_SCAN"
    private val BLUETOOTH_PERMISSIONS_CONNECT = "android.permission.BLUETOOTH_CONNECT"
    private val BLUETOOTH_PERMISSIONS_ADVERTISE = "android.permission.BLUETOOTH_ADVERTISE"

    @OptIn(ExperimentalUnsignedTypes::class)
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
                    (application as TrustChainApplication).initIPv8()
                }
            }
        }

        adapter.registerRenderer(
            DashboardItemRenderer {
                val intent = Intent(this, it.app.activity)
                startActivity(intent)
            }
        )

        setContentView(binding.root)

        val layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        val appList = getAppList().sortedBy { it.app.appName }
        adapter.updateItems(appList)

        ConformanceTest(this, UUID.randomUUID()).run()
    }

    private fun getAppList(): List<DashboardItem> {
        return AppDefinition.values().map {
            DashboardItem(it)
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

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            BLUETOOTH_PERMISSIONS_REQUEST_CODE -> {
                if (hasBluetoothPermissions()) {
                    (application as TrustChainApplication).initIPv8()
                } else {
                    onPermissionsDenied()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private val applicationDetailsSettings = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (hasBluetoothPermissions()) {
            (application as TrustChainApplication).initIPv8()
        } else {
            onPermissionsDenied()
        }
    }

    private fun onPermissionsDenied() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.permissions_denied_message))
            .apply {
                setPositiveButton(getString(R.string.permissions_denied_ok_button)) { _, _ ->
                    run {
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri: Uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        applicationDetailsSettings.launch(intent)
                    }
                }.create()
            }
            .show()
            .setCanceledOnTouchOutside(false)
    }
}
