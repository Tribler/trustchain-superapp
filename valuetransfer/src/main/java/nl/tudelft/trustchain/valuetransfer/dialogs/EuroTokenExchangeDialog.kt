package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import android.widget.CompoundButton
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.dialog_exchange_buy.*
import kotlinx.coroutines.flow.*
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.eurotoken.community.EuroTokenCommunity
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactsFragment
import nl.tudelft.trustchain.valuetransfer.ui.exchange.ExchangeFragment
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.WalletOverviewFragment
import nl.tudelft.trustchain.valuetransfer.util.*


class EuroTokenExchangeDialog(
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
            val view = layoutInflater.inflate(R.layout.dialog_exchange_buy, null)

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

            gateWayName.text = when(gateway != null) {
                true -> gateway.name
                else -> name
            }

            if(!isCreation) {
                view.findViewById<TextView>(R.id.tvBalance).text = (requireActivity() as ValueTransferMainActivity).getBalance(true)
                view.findViewById<TextView>(R.id.tvSellAmount).text = formatBalance(amount!!)
            }

            gateWayPublicKey.text = publicKey.keyToBin().toHex()

            gateWaySaveSwitch.setOnCheckedChangeListener { _, isChecked ->
                gateWayPreferredConstraintLayout.isVisible = isChecked
            }

            val saveGateway = gateWaySaveSwitch.isChecked
            val preferredGateway = gateWayPreferredSwitch.isChecked

            btnConnect.setOnClickListener {
                if(saveGateway) {
                    gatewayStore.addGateway(publicKey, name, ip, port.toLong(), preferredGateway)
                }
                euroTokenCommunity.connectToGateway(publicKey.keyToBin().toHex(), ip, port, paymentID)

                parentActivity.displaySnackbar(requireContext(), "Trying to connect to gateway, continue on exchange portal", isShort = false)
//                parentActivity.displaySnackbar(fragment.requireView(), fragment.requireContext(), "Trying to connect to gateway, continue on exchange portal", isShort = false)
                bottomSheetDialog.dismiss()
            }

            btnContinueSell.setOnClickListener {
                if(saveGateway) {
                    gatewayStore.addGateway(publicKey, name, ip, port.toLong(), preferredGateway)
                }

                val block = transactionRepository.sendDestroyProposalWithPaymentID(
                    publicKey.keyToBin(),
                    ip,
                    port,
                    paymentID,
                    amount!!
                )

                Log.d("VTLOG", block.toString())

                if(block == null) {
                    parentActivity.displaySnackbar(requireContext(), "Sell of ${formatBalance(amount)} ET did not succeed, please try again", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, isShort = false )
//                    parentActivity.displaySnackbar(fragment.requireView(), fragment.requireContext(), "Sell of ${formatBalance(amount)} ET did not succeed, please try again", ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, false)
                }else{
                    parentActivity.displaySnackbar(requireContext(), "${formatBalance(amount)} ET sold to the selected gateway", isShort = false)
//                    parentActivity.displaySnackbar(fragment.requireView(), fragment.requireContext(), "${formatBalance(amount)} ET sold to the selected gateway", isShort = false)
                    bottomSheetDialog.dismiss()
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
