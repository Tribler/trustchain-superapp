package nl.tudelft.trustchain.valuetransfer.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDelegate
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentSettingsBinding

class SettingsFragment : BaseFragment(R.layout.fragment_settings) {
    private val binding by viewBinding(FragmentSettingsBinding::bind)
    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        parentActivity = requireActivity() as ValueTransferMainActivity
        sharedPreferences = parentActivity.getSharedPreferences(ValueTransferMainActivity.preferencesFileName, Context.MODE_PRIVATE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentActivity.toggleActionBar(true)
        parentActivity.setActionBarTitle("Settings", null)
        parentActivity.toggleBottomNavigation(false)

        val theme = sharedPreferences.getString(ValueTransferMainActivity.preferencesThemeName, ValueTransferMainActivity.APP_THEME_DAY)

        binding.switchTheme.isChecked = theme != ValueTransferMainActivity.APP_THEME_DAY
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putString(
                ValueTransferMainActivity.preferencesThemeName,
                when (isChecked) {
                    true -> ValueTransferMainActivity.APP_THEME_NIGHT
                    else -> ValueTransferMainActivity.APP_THEME_DAY
                }
            ).apply()

            when (isChecked) {
                true -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            parentActivity.reloadActivity()
        }
    }

    override fun onResume() {
        super.onResume()

        parentActivity.toggleActionBar(true)
        parentActivity.setActionBarTitle("Settings", null)
        parentActivity.toggleBottomNavigation(false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onBackPressed() {
        val previousFragment = parentFragmentManager.fragments.filter {
            it.tag == ValueTransferMainActivity.walletOverviewFragmentTag
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(0, R.anim.exit_to_right)
            .hide(this)
            .setCustomAnimations(R.anim.enter_from_left, 0)
            .show(previousFragment[0])
            .commit()
        previousFragment[0].onResume()
    }
}
