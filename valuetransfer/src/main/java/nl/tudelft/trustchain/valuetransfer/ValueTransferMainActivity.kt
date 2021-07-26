package nl.tudelft.trustchain.valuetransfer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.main_activity_vt.*
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactChatFragment
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactsFragment
import nl.tudelft.trustchain.valuetransfer.ui.exchange.ExchangeFragment
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityFragment
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.WalletOverviewFragment


class ValueTransferMainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_valuetransfer

    /**
     * All fragments within this application, detail fragments excluded
     */
    private val walletOverviewFragment = WalletOverviewFragment()
    private val identityFragment = IdentityFragment()
    private val exchangeFragment = ExchangeFragment()
    private val contactsFragment = ContactsFragment()

    private val fragmentManager = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity_vt)

        /**
         * On initialisation of activity already load and add all fragments
         */
        fragmentManager.beginTransaction()
            .add(R.id.container, identityFragment, identityFragmentTag).hide(identityFragment)
            .add(R.id.container, exchangeFragment, exchangeFragmentTag).hide(exchangeFragment)
            .add(R.id.container, contactsFragment, contactsFragmentTag).hide(contactsFragment)
            .add(R.id.container, walletOverviewFragment, walletOverviewFragmentTag).commit()

        initListeners()
    }

    /**
     * Reload activity completely
     */
    fun reloadActivity() {
        val refresh = Intent(this, ValueTransferMainActivity::class.java)
        startActivity(refresh)
        finish()
    }

    /**
     * Define bottom navigation view listeners
     */
    private fun initListeners() {
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            val previousTag = fragmentManager.fragments.first {
                it.isVisible
            }.tag

            when (menuItem.itemId) {
                R.id.walletOverviewFragment -> if(previousTag != walletOverviewFragmentTag) pushFragment(walletOverviewFragment)
                R.id.identityFragment -> if(previousTag != identityFragmentTag) pushFragment(identityFragment)
                R.id.exchangeFragment -> if(previousTag != exchangeFragmentTag) pushFragment(exchangeFragment)
                R.id.contactsFragment -> if(previousTag != contactsFragmentTag) pushFragment(contactsFragment)
            }
            true
        }
    }

    /**
     * Controller from fragment to fragment
     */
    fun pushFragment(fragment: Fragment) {
        val previousFragment = fragmentManager.fragments.first {
            it.isVisible
        }

        fragmentManager.beginTransaction()
            .show(fragment)
            .hide(previousFragment)
            .commit()
        fragment.onResume()
    }

    /**
     * Controller from fragment to detail view fragment
     */
    fun detailFragment(tag: String, args: Bundle) {
        val previousFragment = fragmentManager.fragments.first {
            it.isVisible
        }

        when(tag) {
            contactChatFragmentTag -> {
                val contactChatFragment = ContactChatFragment()
                contactChatFragment.arguments = args

                fragmentManager.beginTransaction()
                    .add(R.id.container, contactChatFragment, contactChatFragmentTag)
                    .hide(previousFragment)
                    .commit()
            }
        }
    }

    /**
     * Programmatically push bottom navigation item using tag
     */
    fun selectBottomNavigationItem(tag: String) {
        bottomNavigationView.selectedItemId = when(tag) {
            walletOverviewFragmentTag -> R.id.walletOverviewFragment
            identityFragmentTag -> R.id.identityFragment
            exchangeFragmentTag -> R.id.exchangeFragment
            contactsFragmentTag -> R.id.contactsFragment
            else -> R.id.walletOverviewFragment
        }
    }

    /**
     * Change title of action bar
     */
    fun setActionBarTitle(title: String?) {
        supportActionBar!!.title = title
    }

    /**
     * Switch visibility of action bar
     */
    fun toggleActionBar(state: Boolean) {
        supportActionBar!!.setDisplayHomeAsUpEnabled(state)
        supportActionBar!!.setHomeButtonEnabled(state)
    }

    /**
     * Switch visibility of bottom navigation view
     */
    fun toggleBottomNavigation(state: Boolean) {
        bottomNavigationView.isVisible = state
    }

    companion object {
        const val walletOverviewFragmentTag = "wallet_overview_fragment"
        const val identityFragmentTag = "identity_fragment"
        const val exchangeFragmentTag = "exchange_fragment"
        const val contactsFragmentTag = "contacts_fragment"
        const val contactChatFragmentTag = "contact_chat_fragment"
        const val ARG_PUBLIC_KEY = "public_key"
        const val ARG_NAME = "name"
        const val ARG_PARENT = "parent_tag"

    }
}
