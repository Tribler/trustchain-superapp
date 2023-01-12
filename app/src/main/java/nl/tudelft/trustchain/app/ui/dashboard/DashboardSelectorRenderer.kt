package nl.tudelft.trustchain.app.ui.dashboard

import android.content.res.ColorStateList
import androidx.core.content.res.ResourcesCompat
import com.mattskala.itemadapter.BindingItemRenderer
import nl.tudelft.trustchain.app.databinding.ItemSelectorBinding

class DashboardSelectorRenderer(
    private val onItemClick: (DashboardItem, Boolean) -> Unit
) : BindingItemRenderer<DashboardItem, ItemSelectorBinding>(
    DashboardItem::class.java,
    ItemSelectorBinding::inflate
) {
    override fun bindView(item: DashboardItem, binding: ItemSelectorBinding) {
        val context = binding.root.context
        val color = ResourcesCompat.getColor(context.resources, item.app.color, null)
        binding.imgIcon.setImageResource(item.app.icon)
        if (!item.app.disableImageTint) {
            binding.imgIcon.imageTintList = ColorStateList.valueOf(color)
        }
        binding.txtAppName.text = item.app.appName
        binding.txtAppName.setTextColor(color)
        binding.switchEnable.isChecked = item.isPreferred
        binding.switchEnable.setOnCheckedChangeListener { _, isChecked ->
            onItemClick(item, isChecked)
        }
    }
}
