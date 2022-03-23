package nl.tudelft.trustchain.trader.ui.transfer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import kotlinx.android.synthetic.main.fragment_transfer.*
import kotlinx.android.synthetic.main.fragment_transfer.view.*
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.trader.R

class TransferFragment : BaseFragment() {
    private var isSending = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transfer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("PKPK", trustchain.getMyPublicKey().toHex())
//        txtBalance.text = "Current balance: ${getTrustChainCommunity().getBalance().toString()}"

        transferSendLayout.visibility = View.VISIBLE
        transferReceiveLayout.visibility = View.GONE

        if (arguments?.getString("Public Key") != null) {
            editTxtAddress.setText(arguments?.getString("Public Key"))
        }

        view.QRPK.setImageBitmap(
            QRCodeUtils(
                requireActivity(),
                requireContext()
            ).createQR(trustchain.getMyPublicKey().toHex())
        )

        sendReceiveSwitch.setOnClickListener {
            if (isSending) {
                transferSendLayout.visibility = View.GONE
                transferReceiveLayout.visibility = View.VISIBLE
            } else {
                transferReceiveLayout.visibility = View.GONE
                transferSendLayout.visibility = View.VISIBLE
            }
            isSending = !isSending
        }

        QRPK_Next.setOnClickListener {
            QRCodeUtils(requireActivity(), requireContext()).startQRScanner(this)
//            Temporary QR scan skip
//            val bundle = bundleOf("Proposal Block" to "Prop blockje")
//            view.findNavController().navigate(R.id.action_transferFragment_to_transferReceiveFragment, bundle)
        }

        btnScanPk.setOnClickListener {
            QRCodeUtils(requireActivity(), requireContext()).startQRScanner(this)
        }

        btnSendProposalBlock.setOnClickListener {
            val amount = if (editTxtAmount.text != null && editTxtAmount.text.isNotEmpty()) {
                editTxtAmount.text.toString().toFloat()
            } else {
                0f
            }
            val publicKey = if (editTxtAddress.text != null && editTxtAddress.text.isNotEmpty() &&
                editTxtAddress.text.length % 2 == 0
            ) {
                editTxtAddress.text.toString().hexToBytes()
            } else {
                "null".hexToBytes()
            }
            if (editTxtAddress.text != null && editTxtAmount.text != null &&
                editTxtAmount.text.isNotEmpty() && editTxtAddress.text.isNotEmpty()
            ) {
                if (editTxtAddress.text.length % 2 == 0) {
                    val bundle = bundleOf("Amount" to amount, "Public Key" to publicKey)
                    trustchain.createTxProposalBlock(amount, publicKey)
                    requireView().findNavController()
                        .navigate(R.id.action_transferFragment_to_transferSendFragment, bundle)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "The address is not a valid public key",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * from: https://ariefbayu.xyz/create-barcode-scanner-for-android-using-kotlin-b1a9b1c4d848
     * This function is called when the QR scan has returned a value.
     * If the scan was successful, the appropriate new fragment is loaded, depending on the toggleSwitch
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult? =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) { // This is a result returned by the QR scanner
            val content = result.contents
            if (content != null) {
                if (isSending) {
                    editTxtAddress.setText(content)
                } else {
                    val bundle = bundleOf("Proposal Block" to content)
                    requireView().findNavController()
                        .navigate(R.id.action_transferFragment_to_transferReceiveFragment, bundle)
                }
            } else {
                Log.d("QR Scan", "Scan failed")
            }
        }
    }
}
