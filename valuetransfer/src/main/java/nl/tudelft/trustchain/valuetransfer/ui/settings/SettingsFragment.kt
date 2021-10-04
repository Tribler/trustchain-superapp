package nl.tudelft.trustchain.valuetransfer.ui.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTFragment
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentSettingsBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.OptionsDialog

class SettingsFragment : VTFragment(R.layout.fragment_settings) {
    private val binding by viewBinding(FragmentSettingsBinding::bind)

    private val notificationsStatus = MutableLiveData(NotificationHandler.NOTIFICATION_STATUS_DISABLED)
    private val notificationsMessageStatus = MutableLiveData(NotificationHandler.NOTIFICATION_STATUS_UNKNOWN)
    private val notificationsTransactionStatus = MutableLiveData(NotificationHandler.NOTIFICATION_STATUS_UNKNOWN)

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
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onResume()

        val theme = appPreferences.getCurrentTheme()

        binding.tvSelectedTheme.text = when (theme) {
            AppPreferences.APP_THEME_DAY -> resources.getString(R.string.text_theme_day)
            AppPreferences.APP_THEME_NIGHT -> resources.getString(R.string.text_theme_night)
            AppPreferences.APP_THEME_SYSTEM -> resources.getString(R.string.text_theme_system)
            else -> resources.getString(R.string.text_theme_day)
        }

        binding.clThemeSelector.setOnClickListener {
            OptionsDialog(
                R.menu.settings_theme_options,
                "Choose Theme",
                optionSelected = { _, item ->
                    when (item.itemId) {
                        R.id.actionThemeDay -> AppPreferences.APP_THEME_DAY
                        R.id.actionThemeNight -> AppPreferences.APP_THEME_NIGHT
                        R.id.actionThemeSystem -> AppPreferences.APP_THEME_SYSTEM
                        else -> AppPreferences.APP_THEME_DAY
                    }.run {
                        appPreferences.setTheme(this)
                        appPreferences.switchTheme(this)
                        parentActivity.reloadActivity()
                    }
                }
            ).show(parentFragmentManager, tag)
        }

        notificationsStatus.observe(
            viewLifecycleOwner,
            Observer {
                binding.tvNotificationStatus.text = if (it == NotificationHandler.NOTIFICATION_STATUS_DISABLED) {
                    NotificationHandler.NOTIFICATION_STATUS_DISABLED
                } else ""
                binding.llNotificationsSpecific.isVisible = it == NotificationHandler.NOTIFICATION_STATUS_ENABLED
            }
        )

        notificationsMessageStatus.observe(
            viewLifecycleOwner,
            Observer {
                binding.tvNotificationMessagesStatus.text = it
            }
        )

        notificationsTransactionStatus.observe(
            viewLifecycleOwner,
            Observer {
                binding.tvNotificationTransactionsStatus.text = it
            }
        )

        binding.tvOpenSystemNotificationsSettings.setOnClickListener {
            openChannelSettings()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun openChannelSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, parentActivity.packageName)
                }
                else -> {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", parentActivity.packageName)
                    putExtra("app_uid", parentActivity.applicationInfo.uid)
                }
            }
        }
        startActivity(intent)
        onBackPressed(false)
    }

    override fun onResume() {
        super.onResume()

        parentActivity.apply {
            setActionBarTitle(resources.getString(R.string.menu_navigation_settings), null)
            toggleActionBar(true)
            toggleBottomNavigation(false)
        }

        notificationsStatus.postValue(
            if (NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                NotificationHandler.NOTIFICATION_STATUS_ENABLED
            } else {
                NotificationHandler.NOTIFICATION_STATUS_DISABLED
            }
        )
        notificationsMessageStatus.postValue(
            notificationHandler.getNotificationChannelStatus(
                NotificationHandler.NOTIFICATION_CHANNEL_MESSAGES_ID
            )
        )
        notificationsTransactionStatus.postValue(
            notificationHandler.getNotificationChannelStatus(
                NotificationHandler.NOTIFICATION_CHANNEL_TRANSACTIONS_ID
            )
        )
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

    fun onBackPressed(animated: Boolean = true) {
        val previousFragment = parentFragmentManager.fragments.filter {
            it.tag == ValueTransferMainActivity.walletOverviewFragmentTag
        }

        parentFragmentManager.beginTransaction().apply {
            if (animated) setCustomAnimations(0, R.anim.exit_to_right)
            hide(this@SettingsFragment)
            if (animated) setCustomAnimations(R.anim.enter_from_left, 0)
            show(previousFragment[0])
        }.commit()

        previousFragment[0].onResume()
    }
}
