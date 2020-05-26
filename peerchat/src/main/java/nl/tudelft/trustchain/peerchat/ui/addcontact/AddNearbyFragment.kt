package nl.tudelft.trustchain.peerchat.ui.addcontact

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_add_remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.R
import nl.tudelft.trustchain.peerchat.databinding.FragmentAddNearbyBinding

class AddNearbyFragment : BaseFragment(R.layout.fragment_add_nearby) {
    private val binding by viewBinding(FragmentAddNearbyBinding::bind)

    private var publicKeyBin: String? = null

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myPublicKey = getIpv8().myPeer.publicKey.keyToBin().toHex()
        binding.txtMyPublicKey.text = myPublicKey
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                qrCodeUtils.createQR(myPublicKey)
            }
            binding.qr.setImageBitmap(bitmap)
        }

        binding.contactQr.setOnClickListener {
            qrCodeUtils.startQRScanner(this)
        }

        binding.btnContinue.setOnClickListener {
            val args = Bundle()

            val publicKeyBin = publicKeyBin
            if (publicKeyBin != null) {
                try {
                    defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
                    args.putString(AddContactFragment.ARG_PUBLIC_KEY, publicKeyBin)
                    findNavController().navigate(
                        R.id.action_addNearbyFragment_to_addContactFragment,
                        args
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Invalid public key", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(requireContext(), "Scan the QR code", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val publicKeyBin = qrCodeUtils.parseActivityResult(requestCode, resultCode, data)
        if (publicKeyBin != null) {
            try {
                defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
                setPublicKey(publicKeyBin)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Invalid public key", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Scan failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun setPublicKey(publicKeyBin: String) {
        this.publicKeyBin = publicKeyBin
        binding.txtContactPublicKey.text = publicKeyBin

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                qrCodeUtils.createQR(publicKeyBin)
            }
            binding.contactQr.setImageBitmap(bitmap)
        }
    }
}
