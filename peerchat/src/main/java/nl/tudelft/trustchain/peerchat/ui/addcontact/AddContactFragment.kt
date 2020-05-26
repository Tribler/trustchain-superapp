package nl.tudelft.trustchain.peerchat.ui.addcontact

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_add_contact.*
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
import nl.tudelft.trustchain.peerchat.databinding.FragmentAddContactBinding
import nl.tudelft.trustchain.peerchat.db.PeerChatStore

class AddContactFragment : BaseFragment(R.layout.fragment_add_contact) {
    private val binding by viewBinding(FragmentAddContactBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val publicKeyBin = requireArguments().getString(ARG_PUBLIC_KEY)!!
        binding.txtPublicKey.text = publicKeyBin
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                QRCodeUtils(requireContext()).createQR(publicKeyBin)
            }
            binding.qr.setImageBitmap(bitmap)
        }

        binding.btnSave.setOnClickListener {
            val name = edtName.text.toString()
            if (name.isNotEmpty()) {
                val publicKey = defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())

                val myPublicKey = getIpv8().myPeer.publicKey.keyToBin().toHex()
                if (publicKeyBin != myPublicKey) {
                    PeerChatStore.getInstance(requireContext())
                        .addContact(publicKey, name)
                    findNavController().popBackStack(R.id.contactsFragment, false)
                } else {
                    Toast.makeText(requireContext(), "You cannot add yourself", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Name is empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val ARG_PUBLIC_KEY = "public_key"
    }
}
