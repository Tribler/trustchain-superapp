package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.findNavController
import nl.tudelft.trustchain.common.ui.BaseFragment

class AdminFragment : BaseFragment(R.layout.fragment_admin) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val createTokenButton = view.findViewById<Button>(R.id.buttonTokenCreate)
        createTokenButton.setOnClickListener {
            // Create a new coin and add it to the wallet!
//            creatNewCoin(wallet)
        }

        val buttonTokenList = view.findViewById<Button>(R.id.buttonTokenView)
        buttonTokenList.setOnClickListener {
            val navController = view.findNavController()
            val bundle = Bundle().apply {
                putString("access", "user")
            }
            navController.navigate(R.id.tokenListAdmin, bundle)
        }

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment AdminFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            WalletFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

}
