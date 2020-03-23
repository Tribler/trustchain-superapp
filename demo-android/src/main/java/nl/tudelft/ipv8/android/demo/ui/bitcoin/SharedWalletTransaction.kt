package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import nl.tudelft.ipv8.android.demo.R

/**
 * A simple [Fragment] subclass.
 * Use the [SharedWalletTransaction.newInstance] factory method to
 * create an instance of this fragment.
 */
class SharedWalletTransaction : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val fragment =
            inflater.inflate(R.layout.fragment_shared_wallet_transaction, container, false)
        val args = SharedWalletTransactionArgs.fromBundle(requireArguments())
        fragment.findViewById<TextView>(R.id.public_key_proposal).text = args.publicKey
        fragment.findViewById<TextView>(R.id.voting_threshold_proposal).text = "${args.votingThreshold} %"

        fragment.findViewById<TextView>(R.id.entrance_fee_proposal).text = "${args.entranceFee} BTC"
        fragment.findViewById<TextView>(R.id.users_proposal).text = "${args.users} user(s) in this shared wallet"
        return fragment
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment SharedWalletTransaction.
         */
        @JvmStatic
        fun newInstance() = SharedWalletTransaction()
    }
}
