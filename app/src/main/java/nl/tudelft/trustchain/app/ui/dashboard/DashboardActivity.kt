package nl.tudelft.trustchain.app.ui.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import nl.tudelft.trustchain.common.AppDefinition
import nl.tudelft.trustchain.app.databinding.ActivityDashboardBinding
import nl.tudelft.trustchain.common.util.viewBinding

class DashboardActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityDashboardBinding::inflate)

    private val adapter = ItemAdapter()

    init {
        adapter.registerRenderer(DashboardItemRenderer {})
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        val layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        adapter.updateItems(getAppList())
    }

    private fun getAppList(): List<DashboardItem> {
        return listOf(
            DashboardItem(AppDefinition.TRUSTCHAIN_EXPLORER),
            DashboardItem(AppDefinition.DEBUG)
        )
    }
}
