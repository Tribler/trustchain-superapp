package nl.tudelft.trustchain.offlinemoney.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinemoney.R
import nl.tudelft.trustchain.offlinemoney.databinding.PrintMoneyFragmentBinding
import nl.tudelft.trustchain.offlinemoney.src.RecipientPair
import nl.tudelft.trustchain.offlinemoney.src.Token
import nl.tudelft.trustchain.offlinemoney.db.Token as DBToken

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
            for(i in 1..token1_count){
                tokenpackage+=Token(
                    id=generateRandomString(32),
                    value=1,
                    verifier = generateRandomString(74),
                    genesisHash = generateRandomString(64),
                    recipients= mutableListOf<RecipientPair>())
            }
            for(i in 1..token2_count) {

                    tokenpackage += Token(
                        id = generateRandomString(32),
                        value = 2,
                        verifier = generateRandomString(74),
                        genesisHash = generateRandomString(64),
                        recipients = mutableListOf<RecipientPair>()
                    )

            }
            for(i in 1..token5_count){

                    tokenpackage+=Token(
                        id=generateRandomString(32),
                        value=5,
                        verifier = generateRandomString(74),
                        genesisHash = generateRandomString(64),
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

            lifecycleScope.launch(Dispatchers.IO) {
                for (token in token_package) {
                    db.tokensDao().insertToken(DBToken(token.id.toString(), token.value.toDouble(), Token.serialize(mutableSetOf(token))))
                    for (token_data in Token.deserialize(Token.serialize(mutableSetOf(token)))) {
                        Log.i("db_token", "Token_ID: ${token.id} \t Token value: ${token.value} \t Token_serialize function: ${token_data.id}")
                        break
                    }
//                    Log.i("db_token", "Token_ID: ${token.id} \t Token value: ${token.value} \t Token_serialize function: ${Token.deserialize(Token.serialize(mutableSetOf(token)))}")
                }
            }

            findNavController().navigate(R.id.action_printMoneyFragment_to_transferFragment)
        }

    }
}
