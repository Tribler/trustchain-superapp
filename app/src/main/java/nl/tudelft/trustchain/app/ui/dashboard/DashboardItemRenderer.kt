package nl.tudelft.trustchain.app.ui.dashboard

import android.content.res.ColorStateList
import androidx.core.content.res.ResourcesCompat
import com.mattskala.itemadapter.BindingItemRenderer
import nl.tudelft.trustchain.app.databinding.ItemDashboardBinding

class DashboardItemRenderer(
    private val onItemClick: (DashboardItem) -> Unit
) : BindingItemRenderer<DashboardItem, ItemDashboardBinding>(
    DashboardItem::class.java,
    ItemDashboardBinding::inflate
) {
    override fun bindView(item: DashboardItem, binding: ItemDashboardBinding) {
        val context = binding.root.context
        val color = ResourcesCompat.getColor(context.resources, item.app.color, null)
        binding.imgIcon.setImageResource(item.app.icon)
        binding.imgIcon.imageTintList = ColorStateList.valueOf(color)
        binding.txtAppName.text = item.app.appName
        binding.txtAppName.setTextColor(color)
        binding.root.setOnClickListener {
            onItemClick(item)
        }
    }
}
