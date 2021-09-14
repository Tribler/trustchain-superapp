package nl.tudelft.trustchain.valuetransfer

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBar.LayoutParams
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.androidadvance.topsnackbar.TSnackbar
import com.jaredrummler.blockingdialog.BlockingDialogManager
import kotlinx.android.synthetic.main.main_activity_vt.*
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.WalletAttestation
import nl.tudelft.ipv8.attestation.schema.ID_METADATA
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.TrustChainHelper
import nl.tudelft.trustchain.eurotoken.community.EuroTokenCommunity
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.dialogs.IdentityAttestationConfirmDialog
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactChatFragment
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactsFragment
import nl.tudelft.trustchain.valuetransfer.ui.exchange.ExchangeFragment
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityFragment
import nl.tudelft.trustchain.valuetransfer.ui.settings.SettingsFragment
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.WalletOverviewFragment
import nl.tudelft.trustchain.valuetransfer.util.getColorIDFromThemeAttribute
import org.json.JSONObject

class ValueTransferMainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_valuetransfer

    /**
     * All fragments within this application, detail fragments excluded
     */
    private val walletOverviewFragment = WalletOverviewFragment()
    private val identityFragment = IdentityFragment()
    private val exchangeFragment = ExchangeFragment()
    private val contactsFragment = ContactsFragment()
    private val settingsFragment = SettingsFragment()
    private val qrScanControllerFragment = QRScanController()

    private val fragmentManager = supportFragmentManager

    private lateinit var customActionBar: View
//    private lateinit var titleView: AppCompatTextView
//    private lateinit var subtitleView: AppCompatTextView

    /**
     * Initialize all communities and (database) stores and repo's
     */
    val communities: Map<Class<out Community>, Community> = mapOf(
        TrustChainCommunity::class.java to IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!,
        IdentityCommunity::class.java to IPv8Android.getInstance().getOverlay<IdentityCommunity>()!!,
        PeerChatCommunity::class.java to IPv8Android.getInstance().getOverlay<PeerChatCommunity>()!!,
        EuroTokenCommunity::class.java to IPv8Android.getInstance().getOverlay<EuroTokenCommunity>()!!,
        AttestationCommunity::class.java to IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
    )
    val stores: Map<Any, Any> = mapOf(
        IdentityStore::class.java to IdentityStore.getInstance(this),
        PeerChatStore::class.java to PeerChatStore.getInstance(this),
        GatewayStore::class.java to GatewayStore.getInstance(this),
        ContactStore::class.java to ContactStore.getInstance(this),
        TransactionRepository::class.java to TransactionRepository(getCommunity()!!, GatewayStore.getInstance(this)),
        TrustChainHelper::class.java to TrustChainHelper(getCommunity()!!),
    )

    private var balance = MutableLiveData("0.00")
    private var verifiedBalance = MutableLiveData("0.00")

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        /**
         * Switch to day or night version of theme
         */

        val themePrefs = getSharedPreferences(preferencesFileName, Context.MODE_PRIVATE).getString(preferencesThemeName, APP_THEME_DAY)
        when (themePrefs) {
            APP_THEME_DAY -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            APP_THEME_NIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        super.onCreate(savedInstanceState)

        // Set status bar to black on Lollipop when in day mode
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (themePrefs == APP_THEME_NIGHT) {
                window.statusBarColor = ContextCompat.getColor(
                    applicationContext,
                    getColorIDFromThemeAttribute(
                        this@ValueTransferMainActivity,
                        R.attr.colorPrimary
                    )
                )
            } else {
                window.statusBarColor = Color.BLACK
            }
        }

        setContentView(R.layout.main_activity_vt)

        /**
         * Create database tables if not exist
         */
        val identityCommunity = getCommunity<IdentityCommunity>()!!
        identityCommunity.createIdentitiesTable()
        identityCommunity.createAttributesTable()

        /**
         * On initialisation of activity pre-load all fragments to allow instant switching to increase performance
         */
        fragmentManager.beginTransaction()
            .add(R.id.container, identityFragment, identityFragmentTag).hide(identityFragment)
            .add(R.id.container, exchangeFragment, exchangeFragmentTag).hide(exchangeFragment)
            .add(R.id.container, contactsFragment, contactsFragmentTag).hide(contactsFragment)
            .add(R.id.container, qrScanControllerFragment, qrScanControllerFragmentTag).hide(qrScanControllerFragment)
            .add(R.id.container, settingsFragment, settingsFragmentTag).hide(settingsFragment)
            .add(R.id.container, walletOverviewFragment, walletOverviewFragmentTag).commit()

        /**
         * Create listeners for bottom navigation view
         */
        bottomNavigationViewListeners()

        /**
         * Attestation community callbacks and register own key as trusted authority
         */
        val attestationCommunity = getCommunity<AttestationCommunity>()!!
        attestationCommunity.setAttestationRequestCallback(::attestationRequestCallback)
        attestationCommunity.setAttestationRequestCompleteCallback(::attestationRequestCompleteCallbackWrapper)
        attestationCommunity.setAttestationChunkCallback(::attestationChunkCallback)
        attestationCommunity.trustedAuthorityManager.addTrustedAuthority(IPv8Android.getInstance().myPeer.publicKey)

        /**
         * Create a (centered) custom action bar with a title and subtitle
         */
        customActionBar = LayoutInflater.from(this).inflate(R.layout.action_bar, null)
        customActionBar.findViewById<TextView>(R.id.tv_actionbar_title)
        val params = LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT)
        params.apply {
            gravity = Gravity.CENTER
        }

        supportActionBar?.setCustomView(customActionBar, params)
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
    }

    /**
     * Return a community
     */
    inline fun <reified T : Community> getCommunity(): T? {
        return communities[T::class.java]!! as? T
    }

    /**
     * Returns a store
     */
    inline fun <reified T> getStore(): T? {
        return stores[T::class.java] as? T
    }

    /**
     * Reload/reset activity completely
     */
    fun reloadActivity() {
        finish()
        val intent = Intent(this, ValueTransferMainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
    }

    /**
     * Fix menus shown on wrong fragments
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return false
    }

    /**
     * Define bottom navigation view listeners
     */
    private fun bottomNavigationViewListeners() {
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            val previousTag = fragmentManager.fragments.first {
                it.isVisible
            }.tag

            when (menuItem.itemId) {
                R.id.walletOverviewFragment -> if (previousTag != walletOverviewFragmentTag) switchFragment(walletOverviewFragment)
                R.id.identityFragment -> if (previousTag != identityFragmentTag) switchFragment(identityFragment)
                R.id.exchangeFragment -> if (previousTag != exchangeFragmentTag) switchFragment(exchangeFragment)
                R.id.contactsFragment -> if (previousTag != contactsFragmentTag) switchFragment(contactsFragment)
                R.id.qrScanControllerFragment -> {
                    qrScanControllerFragment.initiateScan()
                    return@setOnNavigationItemSelectedListener false
                }
            }
            true
        }
    }

    /**
     * Controller from fragment to fragment
     */
    private fun switchFragment(fragment: Fragment) {
        val previousFragment = fragmentManager.fragments.first {
            it.isVisible
        }

        fragmentManager.beginTransaction()
            .hide(previousFragment)
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .show(fragment)
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

        when (tag) {
            contactChatFragmentTag -> {
                val contactChatFragment = ContactChatFragment()
                contactChatFragment.arguments = args

                fragmentManager.beginTransaction()
                    .setCustomAnimations(0, R.anim.exit_to_left)
                    .hide(previousFragment)
                    .setCustomAnimations(R.anim.enter_from_right, 0)
                    .add(R.id.container, contactChatFragment, contactChatFragmentTag)
                    .commit()
            }
            settingsFragmentTag -> {
                fragmentManager.beginTransaction()
                    .setCustomAnimations(0, R.anim.exit_to_left)
                    .hide(previousFragment)
                    .setCustomAnimations(R.anim.enter_from_right, 0)
                    .show(settingsFragment)
                    .commit()
                settingsFragment.onResume()
            }
        }
    }

    /**
     * Function that returns the fragment using a given tag
     */
    fun getFragmentByTag(tag: String): Fragment? {
        return when (tag) {
            walletOverviewFragmentTag -> walletOverviewFragment
            identityFragmentTag -> identityFragment
            exchangeFragmentTag -> exchangeFragment
            contactsFragmentTag -> contactsFragment
            settingsFragmentTag -> settingsFragment
            contactChatFragmentTag -> fragmentManager.findFragmentByTag(tag)
            else -> null
        }
    }

    /**
     * Function that closes all currently opened dialogs
     */
    private fun closeAllDialogs() {
        fragmentManager.fragments.filterIsInstance<DialogFragment>().forEach { fragment ->
            fragment.dismissAllowingStateLoss()
        }
    }

    /**
     * Function that returns the rootview of the activity or the containerview that is below the actionbar
     */
    fun getView(isRoot: Boolean = false): View {
        val root = window.decorView.rootView
        if (isRoot) {
            return root
        }

        return root.findViewById(R.id.container)
    }

    /**
     * Function that displays a snackbar at the top in the requested view with the requested text, type and length
     */
    fun displaySnackbar(
        context: Context,
        text: String,
        view: View = getView(),
        type: String = SNACKBAR_TYPE_SUCCESS,
        isShort: Boolean = true,
        extraPadding: Boolean = false
    ) {
        val snackbar = TSnackbar.make(view, text, if (isShort) TSnackbar.LENGTH_SHORT else TSnackbar.LENGTH_LONG)
        val snackbarView = snackbar.view

        snackbar.setActionTextColor(Color.WHITE)

        when (type) {
            SNACKBAR_TYPE_SUCCESS -> snackbarView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryDarkValueTransfer))
            SNACKBAR_TYPE_WARNING -> snackbarView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorYellow))
            SNACKBAR_TYPE_ERROR -> snackbarView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorRed))
        }

        val textView = snackbarView.findViewById<TextView>(com.androidadvance.topsnackbar.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)

        val density = resources.displayMetrics.density

        if (extraPadding) {
            snackbarView.setPadding(snackbarView.paddingLeft, (snackbarView.paddingTop + density * 40).toInt(), snackbarView.paddingRight, snackbarView.paddingBottom)
        }

        snackbar.show()
    }

    /**
     * Programmatically push bottom navigation item using tag
     */
    fun selectBottomNavigationItem(tag: String) {
        bottomNavigationView.selectedItemId = when (tag) {
            walletOverviewFragmentTag -> R.id.walletOverviewFragment
            identityFragmentTag -> R.id.identityFragment
            exchangeFragmentTag -> R.id.exchangeFragment
            contactsFragmentTag -> R.id.contactsFragment
            else -> R.id.walletOverviewFragment
        }
    }

    /**
     * Change title or subtitle of action bar
     */
    fun setActionBarTitle(title: String?, subtitle: String?) {
        customActionBar.findViewById<TextView>(R.id.tv_actionbar_title).text = title
        customActionBar.findViewById<TextView>(R.id.tv_actionbar_subtitle).apply {
            text = subtitle ?: ""
            isVisible = subtitle != null
        }
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

    /**
     * Enable general accessibility for the QRScanController
     */
    fun getQRScanController(): QRScanController {
        return qrScanControllerFragment
    }

    /**
     * Return balance to requested fragment or dialog
     */
    fun getBalance(isVerified: Boolean): MutableLiveData<String> {
        if (isVerified) {
            return verifiedBalance
        }
        return balance
    }

    /**
     * Enable one-time update of balance for complete app
     */
    fun setBalance(balance: String, isVerified: Boolean) {
        if (isVerified) {
            this.verifiedBalance.postValue(balance)
        } else {
            this.balance.postValue(balance)
        }
    }

    /**
     * On receipt of a chunk of the requested attestation execute the following
     */
    private fun attestationChunkCallback(peer: Peer, i: Int) {
        Log.i("VTLOG", "Received attestation chunk $i from ${peer.mid}.")
        Handler(Looper.getMainLooper()).post {
            displaySnackbar(applicationContext, "Received $i attestation chunks from ${peer.mid}.")
        }
    }

    /**
     * After the attestation request has been successfully completed execute the following
     */
    private fun attestationRequestCompleteCallbackWrapper(
        forPeer: Peer,
        attributeName: String,
        attestation: WalletAttestation,
        attributeHash: ByteArray,
        idFormat: String,
        fromPeer: Peer?,
        metaData: String?,
        signature: ByteArray?
    ) {
        attestationRequestCompleteCallback(forPeer, attributeName, attestation, attributeHash, idFormat, fromPeer, metaData, signature, applicationContext)
    }

    @Suppress("UNUSED_PARAMETER")
    fun attestationRequestCompleteCallback(
        forPeer: Peer,
        attributeName: String,
        attestation: WalletAttestation,
        attributeHash: ByteArray,
        idFormat: String,
        fromPeer: Peer?,
        metaData: String?,
        signature: ByteArray?,
        context: Context
    ) {
        if (fromPeer == null) {
            Log.i("VTLOG", "Signed attestation for attribute $attributeName for peer ${forPeer.mid}.")
            Handler(Looper.getMainLooper()).post {
                displaySnackbar(applicationContext, "Successfully sent attestation for $attributeName to peer ${forPeer.mid}", isShort = false)
            }
        } else {
            Log.i("VTLOG", "Received attestation for attribute $attributeName with metadata: $metaData.")
            Handler(Looper.getMainLooper()).post {
                displaySnackbar(applicationContext, "Received Attestation for $attributeName")
            }
        }
    }

    /**
     * On receipt of an attestation request (initiated by the other party) execute the following
     */
    @Suppress("UNUSED_PARAMETER")
    private fun attestationRequestCallback(peer: Peer, attributeName: String, metadata: String): ByteArray {

        closeAllDialogs()

        val parsedMetadata = JSONObject(metadata)
        val idFormat = parsedMetadata.optString("id_format", ID_METADATA)

        val input = BlockingDialogManager.getInstance()
            .showAndWait<String?>(this, IdentityAttestationConfirmDialog(attributeName, idFormat, this))
            ?: throw RuntimeException("User cancelled dialog.")

        Log.i("VTLOG", "Signing attestation with value $input with format $idFormat.")

        Handler(Looper.getMainLooper()).post {
            displaySnackbar(applicationContext, "Signing attestation for $attributeName for peer ${peer.mid} ...", isShort = false)
        }

        Log.d("VTLOG", "CONTINUED")

        return when (idFormat) {
            "id_metadata_range_18plus" -> byteArrayOf(input.toByte())
            else -> input.toByteArray()
        }
    }

    companion object {
        const val walletOverviewFragmentTag = "wallet_overview_fragment"
        const val identityFragmentTag = "identity_fragment"
        const val exchangeFragmentTag = "exchange_fragment"
        const val contactsFragmentTag = "contacts_fragment"
        const val qrScanControllerFragmentTag = "qrscancontroller_fragment"
        const val contactChatFragmentTag = "contact_chat_fragment"
        const val settingsFragmentTag = "settings_fragment"

        const val preferencesFileName = "prefs_vt"
        const val preferencesThemeName = "theme"
        val APP_THEME = R.style.Theme_ValueTransfer
        const val APP_THEME_DAY = "day"
        const val APP_THEME_NIGHT = "night"
//        val APP_THEME_LIGHT = R.style.ThemeLight_ValueTransfer
//        val APP_THEME_DARK = R.style.Theme_ValueTransfer

        const val SNACKBAR_TYPE_SUCCESS = "success"
        const val SNACKBAR_TYPE_WARNING = "warning"
        const val SNACKBAR_TYPE_ERROR = "error"

        const val ARG_PUBLIC_KEY = "public_key"
        const val ARG_NAME = "name"
        const val ARG_PARENT = "parent_tag"
    }
}
