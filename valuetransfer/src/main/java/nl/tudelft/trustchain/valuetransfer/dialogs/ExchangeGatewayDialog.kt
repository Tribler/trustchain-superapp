package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.eurotoken.community.EuroTokenCommunity
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.util.*

class ExchangeGatewayDialog(
    private val isCreation: Boolean,
    private val publicKey: PublicKey,
    private val paymentID: String,
    private val ip: String,
    private val port: Int,
    private val name: String,
    private val amount: Long?,
) : DialogFragment() {

    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var trustChainCommunity: TrustChainCommunity
    private lateinit var euroTokenCommunity: EuroTokenCommunity
    private lateinit var gatewayStore: GatewayStore
    private lateinit var transactionRepository: TransactionRepository

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_exchange_gateway, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.skipCollapsed = true
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            parentActivity = requireActivity() as ValueTransferMainActivity
            trustChainCommunity = parentActivity.getCommunity(ValueTransferMainActivity.trustChainCommunityTag) as TrustChainCommunity
            euroTokenCommunity = parentActivity.getCommunity(ValueTransferMainActivity.euroTokenCommunityTag) as EuroTokenCommunity
            gatewayStore = parentActivity.getStore(ValueTransferMainActivity.gatewayStoreTag) as GatewayStore
            transactionRepository = parentActivity.getStore(ValueTransferMainActivity.transactionRepositoryTag) as TransactionRepository

            val gateWayName = view.findViewById<TextView>(R.id.tvGatewayName)
            val gateWayPublicKey = view.findViewById<TextView>(R.id.tvGatewayPublicKey)
            val gateWaySaveConstraintLayout = view.findViewById<ConstraintLayout>(R.id.clSaveGateway)
            val gateWaySaveSwitch = view.findViewById<Switch>(R.id.switchSaveGateway)
            val gateWayPreferredConstraintLayout = view.findViewById<ConstraintLayout>(R.id.clPreferredGateway)
            val gateWayPreferredSwitch = view.findViewById<Switch>(R.id.switchPreferredGateway)
            val btnConnect = view.findViewById<Button>(R.id.btnConnectToGateway)
            val btnContinueSell = view.findViewById<Button>(R.id.btnContinueSell)

            view.findViewById<TextView>(R.id.tvTitleBuy).isVisible = isCreation
            view.findViewById<TextView>(R.id.tvTitleSell).isVisible = !isCreation
            view.findViewById<ConstraintLayout>(R.id.clBalanceSellAmount).isVisible = !isCreation
            btnConnect.isVisible = isCreation
            btnContinueSell.isVisible = !isCreation

            val gateway = gatewayStore.getGatewayFromPublicKey(publicKey)

            gateWaySaveConstraintLayout.isVisible = gateway == null

            gateWayName.text = when (gateway != null) {
                true -> gateway.name
                else -> name
            }

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

            btnConnect.setOnClickListener {
                btnConnect.text = "Connecting..."

                Handler().postDelayed(
                    Runnable {
                        if (saveGateway) {
                            gatewayStore.addGateway(publicKey, name, ip, port.toLong(), preferredGateway)
                        }

                        euroTokenCommunity.connectToGateway(publicKey.keyToBin().toHex(), ip, port, paymentID)
                        parentActivity.displaySnackbar(requireContext(), "Trying to connect to gateway, continue on exchange portal", isShort = false)
                        bottomSheetDialog.dismiss()
                    },
                    500
                )
            }

            btnContinueSell.setOnClickListener {
                btnContinueSell.text = "Trying to sell..."

                Handler().postDelayed(
                    Runnable {
                        if (saveGateway) {
                            gatewayStore.addGateway(publicKey, name, ip, port.toLong(), preferredGateway)
                        }

                        val block = transactionRepository.sendDestroyProposalWithPaymentID(publicKey.keyToBin(), ip, port, paymentID, amount!!)

                        if (block == null) {
                            btnContinueSell.text = "Sell"
                            parentActivity.displaySnackbar(requireContext(), "Sell of ${formatBalance(amount)} ET did not succeed, please try again", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, isShort = false)
                        } else {
                            parentActivity.displaySnackbar(requireContext(), "${formatBalance(amount)} ET sold to the selected gateway", isShort = false)
                            bottomSheetDialog.dismiss()
                        }
                    },
                    500
                )
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
