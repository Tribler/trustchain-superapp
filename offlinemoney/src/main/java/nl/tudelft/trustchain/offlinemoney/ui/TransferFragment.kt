package nl.tudelft.trustchain.offlinemoney.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinemoney.R
import nl.tudelft.trustchain.offlinemoney.databinding.ActivityMainOfflineMoneyBinding
import org.json.JSONObject
import org.json.JSONException

class TransferFragment : OfflineMoneyBaseFragment(R.layout.activity_main_offline_money) {
    private val binding by viewBinding(ActivityMainOfflineMoneyBinding::bind)

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGet.setOnClickListener {
            qrCodeUtils.startQRScanner(this)
        }

        binding.btnSend.setOnClickListener{
            val amount = getAmount(binding.edtAmount.text.toString())
            println(amount)

            if (amount > 0) {
                val myPeer = "kjalsdjfoiawonsad"

                val connectionData = JSONObject()
                connectionData.put("public_key", myPeer)
                connectionData.put("amount", amount)
                connectionData.put("name", myPeer)


                val args = Bundle()

                args.putString(SendMoneyFragment.ARG_DATA, connectionData.toString())

                findNavController().navigate(
                    R.id.action_transferFragment_to_sendMoneyFragment,
                    args
                )
            }

        }

    }

    fun getAmount(amount: String): Long {
        val regex = """[^\d]""".toRegex()
        if (amount.isEmpty()) {
            return 0L
        }
        return regex.replace(amount, "").toLong()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        qrCodeUtils.parseActivityResult(requestCode, resultCode, data)?.let {
            try {
                val amount = JSONObject(it).optDouble("amount", 0.0)

                val past = binding.txtBalance.text.toString().toDouble()

                binding.txtBalance.text = (past + amount).toString()

            } catch (e: JSONException) {
                Toast.makeText(requireContext(), "Scan failed, try again", Toast.LENGTH_LONG).show()
            }
        } ?: Toast.makeText(requireContext(), "Scan failed", Toast.LENGTH_LONG).show()
        return
    }
}

