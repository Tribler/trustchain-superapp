package nl.tudelft.trustchain.trader.ui.transfer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_transfer_receive.*
import kotlinx.android.synthetic.main.fragment_transfer_receive.view.*
import nl.tudelft.trustchain.trader.R
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.ipv8.attestation.trustchain.TransactionEncoding
import nl.tudelft.ipv8.util.toHex

class TransferReceiveFragment : BaseFragment(R.layout.fragment_transfer_receive) {

    /**
     * Populate confirmation screen with correct public key and amount.
     * Handle next and cancel buttons correctly
     */
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        println("json is " + arguments?.get("Proposal Block"))
        val proposalBlock = TransferBlockParser().stringToProposal((arguments?.get("Proposal Block") as String), trustchain)
        val agreementBlock = trustchain.createAgreementBlock(proposalBlock, proposalBlock.transaction)
        val publicKey = proposalBlock.publicKey.toHex()
        val amount = TransactionEncoding.decode(proposalBlock.rawTransaction)
        textSenderPublicKey.text = "Public key: $publicKey"
        textTransferAmount.text = "Amount: $amount"
        buttonConfirmReceipt.setOnClickListener {
            val view: View = requireView()
            view.transferReceiveLinear.visibility = View.GONE
            view.transferReceiveLinearConfirmed.visibility = View.VISIBLE
            val bitmap: Bitmap? = QRCodeUtils(activity, requireContext())
                .createQR(TransferBlockParser().proposalToString(agreementBlock))
            view.image3rdQR.setImageBitmap(bitmap)
        }
        // Go back to transfer without the ability to go back to this fragment
        buttonCancelReceipt.setOnClickListener {
            view.findNavController().navigate(R.id.action_transferReceiveFragment_to_transferFragment)
        }
        // Go to success animation
        buttonConfirmReceiptTransferEnd.setOnClickListener {
            view.findNavController().navigate(R.id.action_transferReceiveFragment_to_transferConfirmationFragment)
        }
    }
}
