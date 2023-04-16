package nl.tudelft.trustchain.detoks

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import nl.tudelft.trustchain.common.ui.BaseFragment
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentTokenListBinding

class TokenHistoryFragment : BaseFragment(R.layout.fragment_history) {

    private val binding by viewBinding(FragmentTokenListBinding::bind)
    private lateinit var tokenHistory: MutableList<RecipientPair>

    private val adapter = ItemAdapter()
    private val items: LiveData<List<Item>> by lazy {
        liveData { emit(listOf<Item>()) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
        var items = tokenHistory.toList()
        items.observe(viewLifecycleOwner, Observer {
            adapter.updateItems(it)
        })
    }
    companion object {
        private const val EXTRA_STRING_LIST = "com.example.myapp.STRING_LIST"

        fun start(context: Context, tokenHistory: MutableList<RecipientPair>) {
            val intent = Intent(context, TokenHistoryFragment::class.java)
            intent.putExtra(EXTRA_STRING_LIST, tokenHistory.toString())
            context.startActivity(intent)
        }
    }

}
