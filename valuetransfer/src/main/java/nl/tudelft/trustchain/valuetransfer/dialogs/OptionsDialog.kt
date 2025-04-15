package nl.tudelft.trustchain.valuetransfer.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.DialogOptionsBinding
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.dpToPixels
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor

class OptionsDialog(
    private val optionsMenu: Int,
    private val title: String? = null,
    private val bigOptionsEnabled: Boolean? = false,
    private var bigOptionsNumber: Int = 4,
    private val bigOptionsCols: Int = 4,
    private val bigOptionsTextEnabled: Boolean? = true,
    private val bigOptionsIconEnabled: Boolean? = true,
    @SuppressLint("RestrictedApi") // TODO: This is extremely bad practice.
    private val menuMods: ((MenuBuilder) -> MenuBuilder)? = null,
    private val optionSelected: ((BottomSheetDialog, MenuItem) -> Unit)
) : VTDialogFragment() {
    @SuppressLint("RestrictedApi")
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val binding = DialogOptionsBinding.inflate(it.layoutInflater)
            val view = binding.root

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            if (title != null) {
                binding.tvTitle.apply {
                    text = title
                    isVisible = true
                }
            }

            var menu = MenuBuilder(requireContext())
            parentActivity.menuInflater.inflate(optionsMenu, menu)

            menuMods?.invoke(menu)?.let {
                menu = it
            }

            val optionsBigView = binding.llOptionsBig
            optionsBigView.isVisible = bigOptionsEnabled == true

            val originalItems = menu.nonActionItems
            val menuItems =
                if (bigOptionsEnabled == true && bigOptionsNumber > 0) {
                    bigOptionsNumber = kotlin.math.min(bigOptionsNumber, originalItems.size)
                    val items = originalItems.take(kotlin.math.min(bigOptionsNumber, originalItems.size))

                    optionsBigView.weightSum = items.size.toFloat()

                    val layoutRows = mutableListOf<LinearLayout>()

                    val rowCount = (bigOptionsNumber / bigOptionsCols) + if (bigOptionsNumber % bigOptionsCols > 0) 1 else 0
                    for (r in 0 until rowCount) {
                        val row = LinearLayout(requireContext())
                        row.layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                        row.orientation = LinearLayout.HORIZONTAL
                        row.weightSum =
                            when {
                                r < rowCount - 1 -> bigOptionsCols.toFloat()
                                else -> bigOptionsNumber % bigOptionsCols.toFloat()
                            }
                        (row.layoutParams as ViewGroup.MarginLayoutParams).apply {
                            updateMargins(bottom = 8.dpToPixels(requireContext()))
                        }
                        layoutRows.add(row)
                    }

                    for (index in items.indices) {
                        val inflatedView = LayoutInflater.from(requireContext()).inflate(R.layout.item_option_big, null, true)
                        inflatedView.findViewById<TextView>(R.id.tvOptionBig).apply {
                            isVisible = bigOptionsTextEnabled == true
                            if (isVisible) {
                                text = items[index].title
                            }
                        }
                        inflatedView.findViewById<ImageView>(R.id.ivOptionBig).apply {
                            isVisible = bigOptionsIconEnabled == true
                            if (isVisible && items[index].icon != null) {
                                setImageDrawable(items[index].icon)
                            }
                        }
                        inflatedView.setOnClickListener {
                            optionSelected(bottomSheetDialog, items[index])
                            bottomSheetDialog.dismiss()
                        }
                        inflatedView.layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                1.0f
                            )
                        (inflatedView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                            updateMargins(left = 8.dpToPixels(requireContext()))
                        }
                        layoutRows[index / bigOptionsCols].addView(inflatedView)
                    }

                    layoutRows.forEach {
                        optionsBigView.addView(it)
                    }

                    originalItems.takeLast(originalItems.size - items.size)
                } else {
                    originalItems
                }

            binding.listOptions.apply {
                adapter =
                    object : ArrayAdapter<MenuItemImpl>(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        menuItems
                    ) {
                        override fun getView(
                            position: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {
                            return LayoutInflater.from(requireContext()).inflate(R.layout.item_option, null, true).apply {
                                findViewById<TextView>(R.id.optionTitle).text = menuItems[position].title
                                findViewById<ConstraintLayout>(R.id.clOptionIcon).isVisible = menuItems[position].icon != null
                                findViewById<ImageView>(R.id.optionIcon).apply {
                                    if (menuItems[position].icon != null) {
                                        setImageDrawable(menuItems[position].icon)
                                    }
                                }
                            }
                        }
                    }
            }.setOnItemClickListener { _, _, position, _ ->
                optionSelected(bottomSheetDialog, menuItems[position])
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    companion object {
        const val TAG = "options_dialog"
    }
}
