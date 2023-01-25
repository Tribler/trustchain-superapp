package nl.tudelft.trustchain.app.ui.dashboard

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.app.R
import nl.tudelft.trustchain.app.TrustChainApplication
import nl.tudelft.trustchain.app.databinding.ActivityDashboardBinding
import nl.tudelft.trustchain.common.util.viewBinding

@RequiresApi(Build.VERSION_CODES.M)
class DashboardActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityDashboardBinding::inflate)

    private val adapter = ItemAdapter()

    private val BLUETOOTH_PERMISSIONS_REQUEST_CODE = 200
    private val SETTINGS_INTENT_CODE = 1000

    private val BLUETOOTH_PERMISSIONS_SCAN = "android.permission.BLUETOOTH_SCAN"
    private val BLUETOOTH_PERMISSIONS_CONNECT = "android.permission.BLUETOOTH_CONNECT"
    private val BLUETOOTH_PERMISSIONS_ADVERTISE = "android.permission.BLUETOOTH_ADVERTISE"

    override fun onResume() {
        super.onResume()
        adapter.updateItems((application as TrustChainApplication).appLoader.preferredApps)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle init of IPv8 after requesting permissions; only if Android 12 or higher.
        // onPermissionsDenied() is run until user has accepted permissions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

        adapter.updateItems((application as TrustChainApplication).appLoader.preferredApps)

        binding.fab.setOnClickListener {
            val intent = Intent(this, DashboardSelectorActivity::class.java)
            startActivity(intent)
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

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SETTINGS_INTENT_CODE -> {
                if (hasBluetoothPermissions()) {
                    (application as TrustChainApplication).initIPv8()
                } else {
                    onPermissionsDenied()
                }
            }
            else -> {
                @Suppress("DEPRECATION") // TODO: Fix deprecation issue.
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
        @Suppress("DEPRECATION") // TODO: Fix deprecation issue.
        super.onActivityResult(requestCode, resultCode, data)
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
                        @Suppress("DEPRECATION") // TODO: Fix deprecation issue.
                        startActivityForResult(intent, SETTINGS_INTENT_CODE)
                    }
                }.create()
            }
            .show()
            .setCanceledOnTouchOutside(false)
    }
}
