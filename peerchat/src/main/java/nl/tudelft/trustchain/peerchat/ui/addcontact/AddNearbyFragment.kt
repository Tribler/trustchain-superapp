package nl.tudelft.trustchain.peerchat.ui.addcontact

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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

    private var publicKeyBin = MutableLiveData<String>()

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        publicKeyBin.value = savedInstanceState?.getString(KEY_PUBLIC_KEY)
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

            val publicKeyBin = publicKeyBin.value
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

        publicKeyBin.observe(
            viewLifecycleOwner,
            Observer { publicKeyBin ->
                binding.txtContactPublicKey.text = publicKeyBin

                lifecycleScope.launch {
                    val bitmap = if (publicKeyBin != null) withContext(Dispatchers.Default) {
                        qrCodeUtils.createQR(publicKeyBin)
                    } else null
                    binding.contactQr.setImageBitmap(bitmap)
                }
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val publicKeyBin = qrCodeUtils.parseActivityResult(requestCode, resultCode, data)
        if (publicKeyBin != null) {
            try {
                defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
                this.publicKeyBin.value = publicKeyBin
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Invalid public key", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Scan failed", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PUBLIC_KEY, publicKeyBin.value)
    }

    companion object {
        private const val KEY_PUBLIC_KEY = "public_key"
    }
}
