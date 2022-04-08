package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ncorti.slidetoact.SlideToActView
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.*

class ExchangeGatewayDialog(
    private val isCreation: Boolean,
    private val publicKey: PublicKey,
    private val paymentID: String,
    private val ip: String,
    private val port: Int,
    private val name: String,
    private val amount: Long?,
) : VTDialogFragment() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_exchange_gateway, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val gateWayName = view.findViewById<TextView>(R.id.tvGatewayName)
            val gateWayPublicKey = view.findViewById<TextView>(R.id.tvGatewayPublicKey)
            val gateWaySaveConstraintLayout = view.findViewById<ConstraintLayout>(R.id.clSaveGateway)
            val gateWaySaveSwitch = view.findViewById<Switch>(R.id.switchSaveGateway)
            val gateWayPreferredConstraintLayout = view.findViewById<ConstraintLayout>(R.id.clPreferredGateway)
            val gateWayPreferredSwitch = view.findViewById<Switch>(R.id.switchPreferredGateway)

            val connectGatewaySlider = view.findViewById<SlideToActView>(R.id.slideConnectGateway)
            val sellSlider = view.findViewById<SlideToActView>(R.id.slideSell)

            view.findViewById<TextView>(R.id.tvTitleBuy).isVisible = isCreation
            view.findViewById<TextView>(R.id.tvTitleSell).isVisible = !isCreation
            view.findViewById<ConstraintLayout>(R.id.clBalanceSellAmount).isVisible = !isCreation
            connectGatewaySlider.isVisible = isCreation
            sellSlider.isVisible = !isCreation

            val gateway = getGatewayStore().getGatewayFromPublicKey(publicKey)

            gateWaySaveConstraintLayout.isVisible = gateway == null

            gateWayName.text = gateway?.name ?: name

            if (!isCreation) {
                parentActivity.getBalance(true).observe(
                    this,
                    Observer {
                        view.findViewById<TextView>(R.id.tvBalance).text = it
                    }
                )
                view.findViewById<TextView>(R.id.tvSellAmount).text = formatBalance(amount!!)
            }

            gateWayPublicKey.text = publicKey.keyToBin().toHex()

            gateWaySaveSwitch.setOnCheckedChangeListener { _, isChecked ->
                gateWayPreferredConstraintLayout.isVisible = isChecked
            }

            val saveGateway = gateWaySaveSwitch.isChecked
            val preferredGateway = gateWayPreferredSwitch.isChecked

            connectGatewaySlider.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {

                    @Suppress("DEPRECATION")
                    Handler().postDelayed(
                        Runnable {
                            if (saveGateway) {
                                getGatewayStore().addGateway(
                                    publicKey,
                                    name,
                                    ip,
                                    port.toLong(),
                                    preferredGateway
                                )
                            }

                            getEuroTokenCommunity().connectToGateway(
                                publicKey.keyToBin().toHex(),
                                ip,
                                port,
                                paymentID
                            )

                            parentActivity.displayToast(
                                requireContext(),
                                resources.getString(R.string.snackbar_gateway_trying_connect),
                                isShort = false
                            )
                            bottomSheetDialog.dismiss()
                        },
                        500
                    )
                }
            }

            sellSlider.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {
                    @Suppress("DEPRECATION")
                    Handler().postDelayed(
                        Runnable {
                            if (saveGateway) {
                                getGatewayStore().addGateway(publicKey, name, ip, port.toLong(), preferredGateway)
                            }

                            getTransactionRepository().sendDestroyProposalWithPaymentID(publicKey.keyToBin(), ip, port, paymentID, amount!!).let { block ->
                                if (block == null) {
                                    parentActivity.displayToast(
                                        requireContext(),
                                        resources.getString(
                                            R.string.snackbar_exchange_sell_error,
                                            formatBalance(amount)
                                        ),
                                        isShort = false
                                    )
                                } else {
                                    parentActivity.displayToast(
                                        requireContext(),
                                        resources.getString(
                                            R.string.snackbar_exchange_sell_success,
                                            formatBalance(amount)
                                        ),
                                        isShort = false
                                    )
                                    bottomSheetDialog.dismiss()
                                }
                            }
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
