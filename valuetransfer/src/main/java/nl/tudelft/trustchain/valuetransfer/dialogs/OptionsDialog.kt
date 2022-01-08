package nl.tudelft.trustchain.valuetransfer.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor

class OptionsDialog(
    private val optionsMenu: Int,
    private val title: String? = null,
    private val menuMods: ((MenuBuilder) -> MenuBuilder)? = null,
    private val optionSelected: ((BottomSheetDialog, MenuItem) -> Unit)
) : VTDialogFragment() {

    @SuppressLint("RestrictedApi")
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {

            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_options, null)

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            if (title != null) {
                view.findViewById<TextView>(R.id.tvTitle).apply {
                    text = title
                    isVisible = true
                }
            }

            var menu = MenuBuilder(requireContext())
            parentActivity.menuInflater.inflate(optionsMenu, menu)

            menuMods?.invoke(menu)?.let {
                menu = it
            }

            val menuItems = menu.nonActionItems

            view.findViewById<ListView>(R.id.listOptions).apply {
                adapter = object : ArrayAdapter<MenuItemImpl>(
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
