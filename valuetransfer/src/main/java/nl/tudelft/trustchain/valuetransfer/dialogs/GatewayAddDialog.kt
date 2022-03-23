package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.annotation.RequiresApi
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ncorti.slidetoact.SlideToActView
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.*

class GatewayAddDialog(
    private val publicKey: PublicKey,
    private val ip: String,
    private val port: Int,
    private val name: String
) : VTDialogFragment() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_gateway_add, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            // Get the UI views.
            val gateWayName = view.findViewById<TextView>(R.id.tvGatewayName)
            val gateWayPublicKey = view.findViewById<TextView>(R.id.tvGatewayPublicKey)
            val gateWayPreferredSwitch = view.findViewById<Switch>(R.id.switchPreferredGateway)
            val connectGatewaySlider = view.findViewById<SlideToActView>(R.id.slideConnectGateway)

            val gateway = getGatewayStore().getGatewayFromPublicKey(publicKey)

            gateWayName.text = gateway?.name ?: name

            gateWayPublicKey.text = publicKey.keyToBin().toHex()

            // Check if this is the first gateway
            if (getGatewayStore().getGateways().isEmpty()) {
                // If so set preferred to true and disable the switch
                gateWayPreferredSwitch.isEnabled = false
                gateWayPreferredSwitch.isChecked = true
            }
            val preferredGateway = gateWayPreferredSwitch.isChecked

            connectGatewaySlider.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {

                    @Suppress("DEPRECATION")
                    Handler().postDelayed(
                        {
                            // Add the gateway to the store
                            getGatewayStore().addGateway(
                                publicKey,
                                name,
                                ip,
                                port.toLong(),
                                preferredGateway
                            )
                            bottomSheetDialog.dismiss()
                        },
                        500
                    )
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }
}
