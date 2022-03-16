package nl.tudelft.trustchain.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import nl.tudelft.trustchain.app.AppDefinition
import nl.tudelft.trustchain.app.databinding.ActivityDashboardBinding
import nl.tudelft.trustchain.common.ebsi.ConformanceTest
import nl.tudelft.trustchain.common.util.viewBinding
import java.util.*

class DashboardActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityDashboardBinding::inflate)

    private val adapter = ItemAdapter()

    init {
        adapter.registerRenderer(
            DashboardItemRenderer {
                val intent = Intent(this, it.app.activity)
                startActivity(intent)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        val layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        val appList = getAppList().sortedBy { it.app.appName }
        adapter.updateItems(appList)

        ConformanceTest(this, UUID.randomUUID()).run()
    }

    private fun getAppList(): List<DashboardItem> {
        return AppDefinition.values().map {
            DashboardItem(it)
        }
    }
}
