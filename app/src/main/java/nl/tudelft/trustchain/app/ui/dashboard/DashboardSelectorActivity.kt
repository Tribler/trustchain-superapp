package nl.tudelft.trustchain.app.ui.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.runBlocking
import nl.tudelft.trustchain.app.TrustChainApplication
import nl.tudelft.trustchain.app.databinding.FragmentDashboardSelectorBinding
import nl.tudelft.trustchain.common.util.viewBinding

class DashboardSelectorActivity : AppCompatActivity() {
    private val binding by viewBinding(FragmentDashboardSelectorBinding::inflate)
    private val adapter = ItemAdapter()

    override fun onCreate(savedInstanceState: Bundle?) = runBlocking {
        super.onCreate(savedInstanceState)
        title = "Projects"

        adapter.registerRenderer(
            DashboardSelectorRenderer { item, isChecked ->
                runBlocking {
                    if (isChecked) {
                        (application as TrustChainApplication).appLoader.setPreferredApp(item.app.appName)
                    } else {
                        (application as TrustChainApplication).appLoader.removePreferredApp(item.app.appName)
                    }
                }
            }
        )

        setContentView(binding.root)
        val layoutManager = GridLayoutManager(baseContext, 3)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        adapter.updateItems((application as TrustChainApplication).appLoader.apps.toList())
    }
}
