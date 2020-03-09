package nl.tudelft.trustchain.trader.ui.transfer

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.navigation.findNavController
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import kotlinx.android.synthetic.main.fragment_transfer_send.*
import nl.tudelft.trustchain.trader.R
import nl.tudelft.trustchain.common.ui.BaseFragment
import java.lang.IllegalStateException

class TransferSendFragment : BaseFragment(R.layout.fragment_transfer_send) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val receiverPublicKey = arguments?.getByteArray("Public Key")
        val amount = arguments?.getFloat("Amount")

        if (receiverPublicKey != null && amount != null) {
            val block = trustchain.createTxProposalBlock(amount, receiverPublicKey)
            println("Json in is " + TransferBlockParser().proposalToString(block))
            val bitmap: Bitmap? = QRCodeUtils(requireActivity(), requireContext())
                .createQR(TransferBlockParser().proposalToString(block))
            proposalBlockQR.setImageBitmap(bitmap)
            btnProposalScannedNext.setOnClickListener {
                QRCodeUtils(requireActivity(), requireContext()).startQRScanner(this)
            }
        } else {
            // TODO: Error handling when invalid values
            throw IllegalStateException("Invalid transfer receiver or amount")
        }
    }

    /**
     * from: https://ariefbayu.xyz/create-barcode-scanner-for-android-using-kotlin-b1a9b1c4d848
     * This function is called when the QR scan has returned a value.
     * If the scan was successful, the appropriate new fragment is loaded
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if(result != null) { // This is a result returned by the QR scanner
            val content = result.contents
            if(content != null) {
                // TODO: Handle parsing of scanned agreement block
                requireView().findNavController().navigate(R.id.action_transferSendFragment_to_transferConfirmationFragment)
            } else {
                Log.d("QR Scan", "Scan failed")
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
