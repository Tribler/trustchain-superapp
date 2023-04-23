package nl.tudelft.trustchain.offlinedigitaleuro.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
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
import nl.tudelft.trustchain.offlinedigitaleuro.utils.TokenDBUtility
import nl.tudelft.trustchain.offlinedigitaleuro.db.Token as DBToken

class PrintDigitalEuroFragment : OfflineDigitalEuroBaseFragment(R.layout.print_money_fragment) {
    private val binding by viewBinding(PrintMoneyFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.errorText.visibility = View.INVISIBLE
        binding.printNumberPicker1.value = 0
        binding.printNumberPicker2.value = 0
        binding.printNumberPicker5.value = 0
        binding.printNumberPicker10.value = 0

        fun generateRandomString(length:Int):ByteArray{
            val charArray = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            return (1..length).map {charArray.random()}.joinToString("").toByteArray()
        }

        fun createTokens(token1_count:Int, token2_count:Int, token5_count:Int, token10_count:Int): MutableSet<Token> {
            var tokenPackage: MutableSet<Token> = mutableSetOf()

            val resp = RecipientPair(Wallet.CentralAuthority.publicKey.keyToBin(), Wallet.CentralAuthority.privateKey.sign("random signature".toByteArray()))

            for(i in 1..token1_count){
                val token = Token.create(1, Wallet.CentralAuthority.publicKey.keyToBin())
                tokenPackage.add(token)
            }
            for(i in 1..token2_count) {
                val token = Token.create(2, Wallet.CentralAuthority.publicKey.keyToBin())
                tokenPackage.add(token)
            }
            for(i in 1..token5_count){
                val token = Token.create(5, Wallet.CentralAuthority.publicKey.keyToBin())
                tokenPackage.add(token)
            }
            for(i in 1..token10_count){
                val token = Token.create(10, Wallet.CentralAuthority.publicKey.keyToBin())
                tokenPackage.add(token)
            }
            return tokenPackage
        }
        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_printMoneyFragment_to_transferFragment)
        }

        binding.btnPrint.setOnClickListener {
            //This create an array with tokens of values 1,2,5
            val tokenPackage: MutableSet<Token> = createTokens(
                token1_count = binding.printNumberPicker1.value,
                token2_count = binding.printNumberPicker2.value,
                token5_count = binding.printNumberPicker5.value,
                token10_count = binding.printNumberPicker10.value
            )

//            TODO: give tokens from central authority to owner
            for (token in tokenPackage) {
                signByVerifier(token, token.genesisHash, getTrustChainCommunity().myPeer.publicKey.keyToBin())
                var debugMsg = ""
                for (i in 0 until token.numRecipients) {
                    debugMsg += "$i: ${token.recipients[i].publicKey.toHex()}"
                }
                Log.d("ODE", "Debug print: $debugMsg")
            }

            val result = TokenDBUtility.insertToken(tokenPackage.toList(), db)
            val code = result.first
            if (code != TokenDBUtility.Codes.OK) {
                val prevMsg = binding.errorText.text
                val newErrMsg = "Error: failure to print money, reason: $code"
                Log.d("ODE", newErrMsg)
                if (!prevMsg.contains(newErrMsg)) {
                    val errMsg = "$prevMsg\n$newErrMsg"
                    binding.errorText.text = errMsg
                    binding.errorText.visibility = View.VISIBLE
                }

                return@setOnClickListener
            }

            findNavController().navigate(R.id.action_printMoneyFragment_to_transferFragment)
        }

    }

    private fun signByVerifier(token: Token, lastVerifiedProof: ByteArray, recipient: ByteArray): RecipientPair {
        val newRecipientPair = RecipientPair(
            recipient,
            Wallet.CentralAuthority.privateKey.sign(token.id + token.value + lastVerifiedProof + recipient)
        )

        token.genesisHash = lastVerifiedProof
        token.recipients.clear()
        token.recipients.add(newRecipientPair)

        return newRecipientPair
    }
}
