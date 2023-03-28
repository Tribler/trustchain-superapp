package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.navigation.findNavController
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import nl.tudelft.trustchain.common.ui.BaseFragment

class TokenListAdminFragment : BaseFragment(R.layout.fragment_token_list_admin)  {

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf<Item>()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        adapter.registerRenderer(TokenAdminItemRenderer())

        return inflater.inflate(R.layout.fragment_token_list_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonAdminPage = view.findViewById<Button>(R.id.buttonTokenView)
        buttonAdminPage.setOnClickListener {
            val navController = view.findNavController()
            navController.navigate(R.id.tokenListAdmin)
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
