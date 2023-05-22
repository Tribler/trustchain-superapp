package nl.tudelft.trustchain.offlinedigitaleuro.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.navigation.fragment.findNavController
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import org.json.JSONObject
import org.json.JSONException
import nl.tudelft.trustchain.offlinedigitaleuro.R
import nl.tudelft.trustchain.offlinedigitaleuro.databinding.ActivityMainOfflineMoneyBinding

class TransferFragment : OfflineDigitalEuroBaseFragment(R.layout.activity_main_offline_money) {
    private val binding by viewBinding(ActivityMainOfflineMoneyBinding::bind)

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    private fun updateBalance() {
        binding.edt1Euro.text = db.tokensDao().getCountTokensOfValue(1.0).toString()
        binding.edt2Euro.text = db.tokensDao().getCountTokensOfValue(2.0).toString()
        binding.edt5Euro.text = db.tokensDao().getCountTokensOfValue(5.0).toString()
        binding.edt10Euro.text = db.tokensDao().getCountTokensOfValue(10.0).toString()

        var sum = 0

        sum += binding.edt1Euro.text.toString().toInt() * 1 +
            binding.edt2Euro.text.toString().toInt() * 2 +
            binding.edt5Euro.text.toString().toInt() * 5 +
            binding.edt10Euro.text.toString().toInt() * 10

        binding.txtBalance.text = sum.toString()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost = requireActivity() as MenuHost

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.offline_print_option, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.printMoney -> {
                        findNavController().navigate(R.id.action_transferFragment_to_printMoneyFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        runBlocking(Dispatchers.IO) {
            updateBalance()
        }

        binding.btnGet.setOnClickListener {
            qrCodeUtils.startQRScanner(this)
        }

        binding.btnSend.setOnClickListener{
            findNavController().navigate(R.id.action_transferFragment_to_sendAmountFragment)
        }

    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        qrCodeUtils.parseActivityResult(requestCode, resultCode, data)?.let {
            try {
                val transaction = JSONObject(it)

                val args = Bundle()
                args.putString(AcceptEuroFragment.ARG_QR, transaction.toString())

                findNavController().navigate(
                    R.id.action_transferFragment_to_acceptMoneyFragment,
                    args
                )
            } catch (e: JSONException) {
                Toast.makeText(requireContext(), "Scan failed, try again", Toast.LENGTH_LONG).show()
            }
        } ?: Toast.makeText(requireContext(), "Scan failed", Toast.LENGTH_LONG).show()
        return
    }
}


