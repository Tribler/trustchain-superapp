package nl.tudelft.trustchain.offlinemoney.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinemoney.R
import nl.tudelft.trustchain.offlinemoney.databinding.PrintMoneyFragmentBinding
import nl.tudelft.trustchain.offlinemoney.src.RecipientPair
import nl.tudelft.trustchain.offlinemoney.src.Token

class PrintMoneyFragment : OfflineMoneyBaseFragment(R.layout.print_money_fragment) {
    private val binding by viewBinding(PrintMoneyFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.printNumberPicker1.value = 0
        binding.printNumberPicker2.value = 0
        binding.printNumberPicker5.value = 0

        fun generateRandomString(length:Int):ByteArray{
            val chararray: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            return (1..length).map {chararray.random()}.joinToString("").toByteArray()
        }

        fun createTokens(token1_count:Int, token2_count:Int, token5_count:Int): Array<Token> {
            var tokenpackage:Array<Token> = arrayOf<Token>()
            for(i in 0..token1_count){
                tokenpackage+=Token(
                    id=generateRandomString(32),
                    value=1,
                    verifier = generateRandomString(20),
                    genesisHash = generateRandomString(20),
                    recipients= mutableListOf<RecipientPair>())
            }
            for(i in 0..token2_count) {

                    tokenpackage += Token(
                        id = generateRandomString(32),
                        value = 2,
                        verifier = generateRandomString(20),
                        genesisHash = generateRandomString(20),
                        recipients = mutableListOf<RecipientPair>()
                    )

            }
            for(i in 0..token5_count){

                    tokenpackage+=Token(
                        id=generateRandomString(32),
                        value=5,
                        verifier = generateRandomString(20),
                        genesisHash = generateRandomString(20),
                        recipients= mutableListOf<RecipientPair>())
                
            }
            return tokenpackage
        }
        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_printMoneyFragment_to_transferFragment)
        }

        binding.btnPrint.setOnClickListener {
            //This create an array with tokens of values 1,2,5
            val token_package: Array<Token> = createTokens(
                token1_count = binding.printNumberPicker1.value,
                token2_count = binding.printNumberPicker2.value,
                token5_count = binding.printNumberPicker5.value
            )

            findNavController().navigate(R.id.action_printMoneyFragment_to_transferFragment)
        }

    }
}
