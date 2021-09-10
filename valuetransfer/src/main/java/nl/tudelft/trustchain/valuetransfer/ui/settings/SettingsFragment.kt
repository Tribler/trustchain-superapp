package nl.tudelft.trustchain.valuetransfer.ui.settings

import android.os.Bundle
import android.view.*
import kotlinx.android.synthetic.main.fragment_contacts_chat.*
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentSettingsBinding

class SettingsFragment : BaseFragment(R.layout.fragment_settings) {
    private val binding by viewBinding(FragmentSettingsBinding::bind)
    private lateinit var parentActivity: ValueTransferMainActivity

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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentActivity.toggleActionBar(true)
        parentActivity.setActionBarTitle("Settings")
        parentActivity.toggleBottomNavigation(false)

        binding.switchTheme.setOnClickListener {
            parentActivity.displaySnackbar(requireContext(), "Change theme (TODO)")
        }
    }

    override fun onResume() {
        super.onResume()

        parentActivity.toggleActionBar(true)
        parentActivity.setActionBarTitle("Settings")
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
