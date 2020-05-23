package nl.tudelft.trustchain.peerchat

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_add_remote.*
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.databinding.FragmentAddRemoteBinding

class AddRemoteFragment : BaseFragment(R.layout.fragment_add_remote) {
    private val binding by viewBinding(FragmentAddRemoteBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myPublicKey = getIpv8().myPeer.publicKey.keyToBin().toHex()
        binding.txtMyPublicKey.text  = myPublicKey

        btnCopy.setOnClickListener {
            val clipboard =
                ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
            val clip = ClipData.newPlainText("Public Key", myPublicKey)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.btnContinue.setOnClickListener {
            val args = Bundle()
            val publicKeyBin = edtContactPublicKey.text.toString()

            try {
                defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
                args.putString(AddContactFragment.ARG_PUBLIC_KEY, publicKeyBin)
                findNavController().navigate(R.id.action_addRemoteFragment_to_addContactFragment, args)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Invalid public key", Toast.LENGTH_LONG).show()
            }
        }
    }
}
