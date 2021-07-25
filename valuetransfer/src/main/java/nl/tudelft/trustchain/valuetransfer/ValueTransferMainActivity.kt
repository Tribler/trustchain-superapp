package nl.tudelft.trustchain.valuetransfer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
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
    private var contactChatFragment = ContactChatFragment()

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

    private fun initListeners() {
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.walletOverviewFragment -> pushFragment(walletOverviewFragment, walletOverviewFragmentTag)
                R.id.identityFragment -> pushFragment(identityFragment, identityFragmentTag)
                R.id.exchangeFragment -> pushFragment(exchangeFragment, exchangeFragmentTag)
                R.id.contactsFragment -> pushFragment(contactsFragment, contactsFragmentTag)
            }
            true
        }
    }

    /**
     * Controller from fragment to fragment
     */
    fun pushFragment(fragment: Fragment, tag: String) {
        val fragmentTransaction = fragmentManager.beginTransaction()

        val previousFragment = fragmentManager.fragments.first {
            it.isVisible
        }

        if (fragmentManager.findFragmentByTag(tag) == null) {
            fragmentTransaction
                .add(R.id.container, fragment, tag)
                .hide(previousFragment)
                .commit()
        } else {
            fragmentTransaction
                .show(fragment)
                .hide(previousFragment)
                .commit()
            fragment.onResume()
        }
    }

//    fun switchToFragment(tag: String) {
//        when (tag) {
//            walletOverviewFragmentTag -> pushFragment(walletOverviewFragment, tag)
//            identityFragmentTag -> pushFragment(identityFragment, tag)
//            exchangeFragmentTag -> pushFragment(exchangeFragment, tag)
//            contactsFragmentTag -> pushFragment(contactsFragment, tag)
//            contactChatFragmentTag -> pushFragment(contactChatFragment, tag)
//        }
//    }

    /**
     * Controller from fragment to detail view fragment
     */
    fun detailFragment(tag: String, args: Bundle) {
        val previousFragment = fragmentManager.fragments.first {
            it.isVisible
        }

        when(tag) {
            contactChatFragmentTag -> {
                if(fragmentManager.findFragmentByTag(contactChatFragmentTag) == null) {
                    Log.d("TESTJE", "CONTACT CHAT NOT LOADED BEFORE")

                    contactChatFragment = ContactChatFragment()
                    contactChatFragment.arguments = args

                    fragmentManager.beginTransaction()
                        .add(R.id.container, contactChatFragment, contactChatFragmentTag)
                        .hide(previousFragment)
                        .commit()
                }else{
                    Log.d("TESTJE", "CONTACT CHAT LOADED BEFORE")
                    contactChatFragment.arguments = args
                    pushFragment(contactChatFragment, contactChatFragmentTag)
                }
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

    fun setActionBarTitle(title: String?) {
        supportActionBar!!.title = title
    }

    fun toggleActionBar(state: Boolean) {
        supportActionBar!!.setDisplayHomeAsUpEnabled(state)
        supportActionBar!!.setHomeButtonEnabled(state)
    }

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
