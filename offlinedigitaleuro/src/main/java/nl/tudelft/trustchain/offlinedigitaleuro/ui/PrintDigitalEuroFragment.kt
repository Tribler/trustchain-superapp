package nl.tudelft.trustchain.offlinedigitaleuro.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinedigitaleuro.src.RecipientPair
import nl.tudelft.trustchain.offlinedigitaleuro.src.Token
import nl.tudelft.trustchain.offlinedigitaleuro.src.Wallet
import nl.tudelft.trustchain.offlinedigitaleuro.R
import nl.tudelft.trustchain.offlinedigitaleuro.databinding.PrintMoneyFragmentBinding
import nl.tudelft.trustchain.offlinedigitaleuro.db.Token as DBToken

class PrintDigitalEuroFragment : OfflineDigitalEuroBaseFragment(R.layout.print_money_fragment) {
    private val binding by viewBinding(PrintMoneyFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.printNumberPicker1.value = 0
        binding.printNumberPicker2.value = 0
        binding.printNumberPicker5.value = 0
        binding.printNumberPicker10.value = 0

        fun generateRandomString(length:Int):ByteArray{
            val charArray = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            return (1..length).map {charArray.random()}.joinToString("").toByteArray()
        }

        fun createTokens(token1_count:Int, token2_count:Int, token5_count:Int, token10_count:Int): Array<Token> {
            var tokenPackage:Array<Token> = arrayOf()

            val resp = RecipientPair(Wallet.CentralAuthority.publicKey.keyToBin(), Wallet.CentralAuthority.privateKey.sign("random signature".toByteArray()))

            for(i in 1..token1_count){
                tokenPackage+=Token(
                    id = generateRandomString(8),
                    value = 1,
                    verifier = generateRandomString(74),
                    genesisHash = generateRandomString(64),
                    recipients= mutableListOf(resp))
            }
            for(i in 1..token2_count) {
                tokenPackage += Token(
                    id = generateRandomString(8),
                    value = 2,
                    verifier = generateRandomString(74),
                    genesisHash = generateRandomString(64),
                    recipients = mutableListOf(resp)
                )

            }
            for(i in 1..token5_count){
                tokenPackage+=Token(
                    id=generateRandomString(8),
                    value=5,
                    verifier = generateRandomString(74),
                    genesisHash = generateRandomString(64),
                    recipients= mutableListOf(resp)
                )
            }
            for(i in 1..token10_count){
                tokenPackage+=Token(
                    id = generateRandomString(8),
                    value = 10,
                    verifier = generateRandomString(74),
                    genesisHash = generateRandomString(64),
                    recipients= mutableListOf(resp)
                )
            }
            return tokenPackage
        }
        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_printMoneyFragment_to_transferFragment)
        }

        binding.btnPrint.setOnClickListener {
            //This create an array with tokens of values 1,2,5
            val tokenPackage: Array<Token> = createTokens(
                token1_count = binding.printNumberPicker1.value,
                token2_count = binding.printNumberPicker2.value,
                token5_count = binding.printNumberPicker5.value,
                token10_count = binding.printNumberPicker10.value
            )

            lifecycleScope.launch(Dispatchers.IO) {
                for (token in tokenPackage) {
                    dbUtility.recieve(token, requireContext())
                }
            }

            findNavController().navigate(R.id.action_printMoneyFragment_to_transferFragment)
        }

    }
}
