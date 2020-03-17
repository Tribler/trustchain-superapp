package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import nl.tudelft.ipv8.android.demo.R

private const val PUBLIC_KEY = "publicKey"
private const val VOTING_THRESHOLD = "votingThreshold"
private const val ENTRANCE_FEE = "entranceFee"
private const val USERS = "users"


/**
 * A simple [Fragment] subclass.
 * Use the [JoinNetworkSteps.newInstance] factory method to
 * create an instance of this fragment.
 */
class JoinNetworkSteps : Fragment() {
    private var publicKey: String = ""
    private var votingThreshold: String = ""
    private var entranceFee: String = ""
    private var users: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            publicKey = it.getString(PUBLIC_KEY, PUBLIC_KEY)
            votingThreshold = it.getString(VOTING_THRESHOLD, VOTING_THRESHOLD)
            entranceFee = it.getString(ENTRANCE_FEE, ENTRANCE_FEE)
            users = it.getString(USERS, USERS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_join_network_steps, container, false)
        view.findViewById<TextView>(R.id.public_key_proposal).text = publicKey
        view.findViewById<TextView>(R.id.voting_threshold_proposal).text = votingThreshold
        view.findViewById<TextView>(R.id.entrance_fee_proposal).text = entranceFee
        view.findViewById<TextView>(R.id.users_proposal).text = users
        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment joinNetworkSteps.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(
            publicKey: String,
            votingThreshold: String,
            entranceFee: String,
            users: String
        ) =
            JoinNetworkSteps().apply {
                arguments = Bundle().apply {
                    putString(PUBLIC_KEY, publicKey)
                    putString(VOTING_THRESHOLD, votingThreshold)
                    putString(ENTRANCE_FEE, entranceFee)
                    putString(USERS, users)
                }
            }
    }
}
