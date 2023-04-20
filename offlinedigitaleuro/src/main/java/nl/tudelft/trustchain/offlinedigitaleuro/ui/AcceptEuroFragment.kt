package nl.tudelft.trustchain.offlinedigitaleuro.ui

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.accept_euro_fragment.*
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinedigitaleuro.R
import nl.tudelft.trustchain.offlinedigitaleuro.databinding.AcceptEuroFragmentBinding
import nl.tudelft.trustchain.offlinedigitaleuro.payloads.TransferQR
import org.json.JSONObject

class AcceptEuroFragment : OfflineDigitalEuroBaseFragment(R.layout.accept_euro_fragment) {
    private val binding by viewBinding(AcceptEuroFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val qr = TransferQR.fromJson(JSONObject(requireArguments().getString(ARG_QR)!!))!!
        val publicKey = qr.pvk.toString()
        var amount = 0

        for (token in qr.tokens) {
            amount += token.value.toInt()
        }

        binding.txtAmount.text = "€" + (amount / 100).toString() + "," + (Math.abs(amount) % 100).toString()
            .padStart(2, '0')
        binding.txtContactPublicKey.text = publicKey

        val trustScore = db.webOfTrustDao().getUserTrustScore(publicKey)
        logger.info { "Trustscore: $trustScore" }

        if (trustScore != null) {
            if (trustScore >= 50) {
                trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_high, trustScore)
                trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.android_green))
            } else if (trustScore > 10) {
                trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_average, trustScore)
                trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.metallic_gold))
            } else {
                trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_low, trustScore)
                trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
            }
        } else {
            trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_no_score)
            trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.metallic_gold))
            trustScoreWarning.visibility = View.VISIBLE
        }

        // TODO: maybe check for each token that if we already have and for each that is smaller in chain we -1 to trust score to the users in chain(and -10 for LCA)

        // TODO: Maybe something to add to a code like this? and insert the good ones and not insert the bad ones => good ones +1 to users in chain and add users

        //                runBlocking(Dispatchers.IO) {
//                    for (token in qr.tokens) {
//                        db.tokensDao().insertToken(
//                            nl.tudelft.trustchain.offlinedigitaleuro.db.Token(
//                                token.id.toHex(),
//                                token.value.toDouble(),
//                                Token.serialize(mutableSetOf(token))
//                            )
//                        );
//                    }
//
//                    updateBalance()
//                }

        binding.btnAccept.setOnClickListener {
            // TODO: add logic here for checking tokens?
            findNavController().navigate(R.id.action_acceptMoneyFragment_to_transferFragment)
        }

        binding.btnRefuse.setOnClickListener {
            findNavController().navigate(R.id.action_acceptMoneyFragment_to_transferFragment)
        }
    }

    companion object {
        const val ARG_QR = "qr"
    }
}
