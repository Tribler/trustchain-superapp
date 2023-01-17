package nl.tudelft.trustchain.valuetransfer

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.androidadvance.topsnackbar.TSnackbar
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.jaredrummler.blockingdialog.BlockingDialogManager
import kotlinx.android.synthetic.main.dialog_image.*
import kotlinx.android.synthetic.main.main_activity_vt.*
import kotlinx.coroutines.*
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.WalletAttestation
import nl.tudelft.ipv8.attestation.schema.ID_METADATA
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.TrustChainHelper
import nl.tudelft.trustchain.common.valuetransfer.extensions.bytesToImage
import nl.tudelft.trustchain.common.valuetransfer.extensions.decodeBytes
import nl.tudelft.trustchain.common.valuetransfer.extensions.imageBytes
import nl.tudelft.trustchain.common.valuetransfer.extensions.resize
import nl.tudelft.trustchain.eurotoken.community.EuroTokenCommunity
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.entity.ContactImage
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.dialogs.IdentityAttestationConfirmDialog
import nl.tudelft.trustchain.valuetransfer.passport.PassportHandler
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.valuetransfer.ui.VTFragment
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactChatFragment
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactsFragment
import nl.tudelft.trustchain.valuetransfer.ui.exchange.ExchangeFragment
import nl.tudelft.trustchain.valuetransfer.ui.exchangelink.ExchangeTransferMoneyLinkFragment
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityFragment
import nl.tudelft.trustchain.valuetransfer.ui.settings.AppPreferences
import nl.tudelft.trustchain.valuetransfer.ui.settings.NotificationHandler
import nl.tudelft.trustchain.valuetransfer.ui.settings.SettingsFragment
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.WalletOverviewFragment
import nl.tudelft.trustchain.valuetransfer.util.dpToPixels
import nl.tudelft.trustchain.valuetransfer.util.getColorIDFromThemeAttribute
import org.json.JSONObject
import java.util.*

class ValueTransferMainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_valuetransfer

    /**
     * All fragments within this application, contact chat fragment excluded because it depends on arguments
     */
    private val fragmentManager = supportFragmentManager
    private val walletOverviewFragment = WalletOverviewFragment()
    private val identityFragment = IdentityFragment()
    private val exchangeFragment = ExchangeFragment()
    private val contactsFragment = ContactsFragment()
    private val settingsFragment = SettingsFragment()
    private val exchangeTransferMoneyLinkFragment = ExchangeTransferMoneyLinkFragment()
    private val qrScanController = QRScanController()

    private lateinit var customActionBar: View
    private lateinit var notificationHandler: NotificationHandler
    private lateinit var appPreferences: AppPreferences
    private lateinit var passportHandler: PassportHandler

    private var balance = MutableLiveData("0.00")
    private var verifiedBalance = MutableLiveData("0.00")

    private var isAppInForeground = true

    /**
     * Initialize all communities and (database) stores and repo's for performance purposes and
     * ease of use in other classes/fragments.
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

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        /**
         * Initialize app preferences class and set theme saved in it
         */
        appPreferences = AppPreferences.getInstance(this)
        val currentTheme = appPreferences.getCurrentTheme()
        appPreferences.switchTheme(currentTheme)

        super.onCreate(savedInstanceState)

        // Set status bar to black on Lollipop when in day mode
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = if (currentTheme == AppPreferences.APP_THEME_NIGHT) {
                ContextCompat.getColor(
                    applicationContext,
                    getColorIDFromThemeAttribute(
                        this@ValueTransferMainActivity,
                        R.attr.colorPrimary
                    )
                )
            } else {
                Color.BLACK
            }
        }
        setContentView(R.layout.main_activity_vt)
        val action: String? = intent?.action
        val data: Uri? = intent?.data
        var requestMoney = false
        if (action != null && data != null) {
            requestMoney = exchangeTransferMoneyLinkFragment.handleLinkRequest(data)
        }

        // Create identity database tables if not exist
        val identityCommunity = getCommunity<IdentityCommunity>()!!
        identityCommunity.createIdentitiesTable()
        identityCommunity.createAttributesTable()

        /**
         * Initialize notification and passport handler
         */
        notificationHandler = NotificationHandler.getInstance(this)
        passportHandler = PassportHandler.getInstance(this)

        /**
         * Detect foreground/background state changes for handling notifications
         */
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event: Lifecycle.Event ->
                val temp = isAppInForeground
                isAppInForeground = listOf(
                    Lifecycle.Event.ON_CREATE,
                    Lifecycle.Event.ON_START,
                    Lifecycle.Event.ON_RESUME
                ).contains(event)

                if (temp != isAppInForeground && isAppInForeground) notificationHandler.cancelAll()
                Log.d("VTLOG", "FOREGROUND: $isAppInForeground")
            }
        )

        /**
         * On initialisation of activity pre-load all fragments to allow instant switching to increase performance
         */
        if (!requestMoney)
            fragmentManager.beginTransaction()
                .add(R.id.container, identityFragment, identityFragmentTag).hide(identityFragment)
                .add(R.id.container, exchangeFragment, exchangeFragmentTag).hide(exchangeFragment)
                .add(R.id.container, contactsFragment, contactsFragmentTag).hide(contactsFragment)
                .add(R.id.container, qrScanController, qrScanControllerTag).hide(qrScanController)
                .add(R.id.container, settingsFragment, settingsFragmentTag).hide(settingsFragment)
                .add(R.id.container, exchangeTransferMoneyLinkFragment, exchangeTransferMoneyLinkFragmentTag).hide(exchangeTransferMoneyLinkFragment)
                .add(R.id.container, walletOverviewFragment, walletOverviewFragmentTag)
                .commit()
        else
            fragmentManager.beginTransaction()
                .add(R.id.container, identityFragment, identityFragmentTag).hide(identityFragment)
                .add(R.id.container, exchangeFragment, exchangeFragmentTag).hide(exchangeFragment)
                .add(R.id.container, contactsFragment, contactsFragmentTag).hide(contactsFragment)
                .add(R.id.container, qrScanController, qrScanControllerTag).hide(qrScanController)
                .add(R.id.container, settingsFragment, settingsFragmentTag).hide(settingsFragment)
                .add(R.id.container, exchangeTransferMoneyLinkFragment, exchangeTransferMoneyLinkFragmentTag)
                .add(R.id.container, walletOverviewFragment, walletOverviewFragmentTag).hide(walletOverviewFragment)
                .commit()
        fragmentManager.executePendingTransactions()

        /**
         * Bottom navigation view listener and set icon of qr scan controller the height without title
         */
        bottomNavigationViewListeners()

        val bottomView: BottomNavigationMenuView =
            bottomNavigationView.getChildAt(0) as BottomNavigationMenuView
        (bottomView.getChildAt(2) as BottomNavigationItemView).setIconSize(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 40f,
                resources.displayMetrics
            ).toInt()
        )

        /**
         * PeerChat community callbacks and creating additional contact database tables
         */
        val peerChatCommunity = getCommunity<PeerChatCommunity>()!!
        peerChatCommunity.setOnMessageCallback(::onMessageCallback)
        peerChatCommunity.setOnContactImageRequestCallback(::onContactImageRequestCallback)
        peerChatCommunity.setOnContactImageCallback(::onContactImageCallback)
        peerChatCommunity.createContactStateTable()
        peerChatCommunity.createContactImageTable()
        peerChatCommunity.identityInfo = identityCommunity.getIdentityInfo(appPreferences.getIdentityFaceHash())

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
        setActionBarWithGravity(Gravity.CENTER)
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setDefaultDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)

        /**
         * Enable click on notification when app is currently not on foreground
         */
        if (intent != null && identityCommunity.hasIdentity() && !lifecycle.currentState.isAtLeast(
                Lifecycle.State.RESUMED
            )
        ) {
            onNewIntent(intent)
        }

        checkCameraPermissions()
    }

    /**
     * Detect foreground status of app
     */
    override fun onStart() {
        super.onStart()
        isAppInForeground = true
    }

    /**
     * Detect foreground status of app
     */
    override fun onStop() {
        super.onStop()
        isAppInForeground = false
    }

    /**
     * Enable NFC scanning
     */
    override fun onResume() {
        super.onResume()

        isAppInForeground = true

        if (passportHandler.getNFCAdapter() != null) {
            val intent = Intent(applicationContext, this.javaClass)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, javaClass).addFlags(
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, javaClass).addFlags(
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            val filter = arrayOf(arrayOf("android.nfc.tech.IsoDep"))
            passportHandler.getNFCAdapter()!!.enableForegroundDispatch(this, pendingIntent, null, filter)
        }
    }

    /**
     * Only allow NFC scanning when the app is on the foreground
     */
    override fun onPause() {
        super.onPause()

        isAppInForeground = false

        passportHandler.getNFCAdapter()?.disableForegroundDispatch(this)
    }

    /**
     * Enable notification click intents
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent == null) return

        when {
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED -> nfcIntentController(intent)
            getCommunity<IdentityCommunity>()!!.hasIdentity() -> {

                @Suppress("DEPRECATION")
                Handler().postDelayed(
                    {
                        notificationIntentController(intent)
                    },
                    1000
                )
            }
        }
    }

    /**
     * Process an incoming NFC tag intent
     */
    private fun nfcIntentController(intent: Intent) {
        intent.extras?.getParcelable<Tag>(NfcAdapter.EXTRA_TAG)?.let { tag ->
            if (tag.techList.contains("android.nfc.tech.IsoDep")) {
                passportHandler.setIsoDep(tag)
            }
        }
    }

    /**
     * Controls the behaviour of the notification click intents to the correct fragment
     */
    private fun notificationIntentController(intent: Intent) {
        val fragmentTag = intent.extras?.getString(ARG_FRAGMENT)

        if (fragmentTag != null) {
            closeAllDialogs()

            when (fragmentTag) {
                contactChatFragmentTag -> {
                    when (val fragment = notificationChatIntent(intent)) {
                        is ContactChatFragment -> {
                            val previousFragmentTag = fragment.arguments?.getString(ARG_PARENT) ?: walletOverviewFragmentTag
                            val previousFragment = getFragmentByTag(previousFragmentTag)!!

                            if (previousFragmentTag == contactChatFragmentTag) {
                                if (previousFragment.arguments?.getString(ARG_PUBLIC_KEY) != fragment.arguments?.getString(ARG_PUBLIC_KEY)) {
                                    fragment.arguments = fragment.arguments?.apply {
                                        putString(ARG_PARENT, previousFragment.arguments?.getString(ARG_PARENT))
                                    }

                                    fragmentManager.beginTransaction().apply {
                                        remove(previousFragment)
                                        add(R.id.container, fragment, fragmentTag)
                                    }.commit()
                                }
                            } else {
                                fragmentManager.beginTransaction().apply {
                                    hide(previousFragment)
                                    add(R.id.container, fragment, fragmentTag)
                                }.commit()
                            }
                        }
                        is ContactsFragment -> {
                            selectBottomNavigationItem(contactsFragmentTag)
                        }
                    }
                }
                else -> {
                    getActiveFragment()?.let { previousFragment ->
                        if (previousFragment is ContactChatFragment) {
                            previousFragment.onBackPressed()
                        } else if (previousFragment is SettingsFragment) {
                            previousFragment.onBackPressed(false)
                        }
                    }

                    fragmentManager.executePendingTransactions()

                    when (fragmentTag) {
                        settingsFragmentTag -> {
                            detailFragment(settingsFragmentTag, Bundle())
                        }
                        exchangeTransferMoneyLinkFragmentTag -> {
                            detailFragment(exchangeTransferMoneyLinkFragmentTag, Bundle())
                        }
                        else -> {
                            selectBottomNavigationItem(fragmentTag)
                            (getFragmentByTag(fragmentTag)!! as VTFragment).initView()
                        }
                    }
                }
            }
        }

        notificationHandler.cancelAll()
    }

    /**
     * Get the fragment that is currently active/shown (if exists)
     */
    fun getActiveFragment(): Fragment? {
        return fragmentManager.fragments.firstOrNull {
            it.isVisible
        }
    }

    /**
     * Get the chat or contacts fragment from a notification intent
     */
    private fun notificationChatIntent(intent: Intent): Fragment {
        val publicKeyString = intent.extras?.getString(ARG_PUBLIC_KEY)
        val activeFragment = getActiveFragment()

        return if (publicKeyString != null) {
            try {
                val publicKey = defaultCryptoProvider.keyFromPublicBin(publicKeyString.hexToBytes())
                val contact = getStore<ContactStore>()!!.getContactFromPublicKey(publicKey)
                val identityName = getStore<PeerChatStore>()!!.getContactState(publicKey)?.identityInfo?.let {
                    "${it.initials} ${it.surname}"
                }

                ContactChatFragment().apply {
                    arguments = bundleOf(
                        ARG_PUBLIC_KEY to publicKeyString,
                        ARG_NAME to (contact?.name ?: (identityName ?: resources.getString(R.string.text_unknown_contact))),
                        ARG_PARENT to (activeFragment?.tag ?: walletOverviewFragmentTag)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                contactsFragment
            }
        } else {
            contactsFragment
        }
    }

    /**
     * Return the instance of notification handler
     */
    fun notificationHandler(): NotificationHandler {
        return notificationHandler
    }

    /**
     * Return the instance of the passport handler
     */
    fun passportHandler(): PassportHandler {
        return passportHandler
    }

    /**
     * Return the instance of app preferences class
     */
    fun appPreferences(): AppPreferences {
        return appPreferences
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
            val activeTag = getActiveFragment()?.tag

            when (menuItem.itemId) {
                R.id.walletOverviewFragment -> if (activeTag != walletOverviewFragmentTag) switchFragment(walletOverviewFragment)
                R.id.identityFragment -> if (activeTag != identityFragmentTag) switchFragment(identityFragment)
                R.id.exchangeFragment -> if (activeTag != exchangeFragmentTag) switchFragment(exchangeFragment)
                R.id.contactsFragment -> if (activeTag != contactsFragmentTag) switchFragment(contactsFragment)
                R.id.qrScanControllerFragment -> {
                    qrScanController.initiateScan()
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
        val activeFragment = getActiveFragment()

        fragmentManager.beginTransaction().apply {
            if (activeFragment != null) hide(activeFragment)
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            show(fragment)
        }.commit()

        (fragment as VTFragment).initView()
    }

    /**
     * Controller from fragment to detail view fragment
     */
    fun detailFragment(tag: String, args: Bundle) {
        val activeFragment = getActiveFragment()

        when (tag) {
            contactChatFragmentTag -> {
                val contactChatFragment = ContactChatFragment()
                contactChatFragment.arguments = args

                fragmentManager.beginTransaction().apply {
                    setCustomAnimations(0, R.anim.exit_to_left)
                    if (activeFragment != null) hide(activeFragment)
                    setCustomAnimations(R.anim.enter_from_right, 0)
                    add(R.id.container, contactChatFragment, contactChatFragmentTag)
                }.commit()
            }
            settingsFragmentTag -> {
                fragmentManager.beginTransaction().apply {
                    setCustomAnimations(0, R.anim.exit_to_left)
                    if (activeFragment != null) hide(activeFragment)
                    setCustomAnimations(R.anim.enter_from_right, 0)
                    show(settingsFragment)
                }.commit()

                (settingsFragment as VTFragment).initView()
            }
            exchangeTransferMoneyLinkFragmentTag -> {
                fragmentManager.beginTransaction().apply {
                    setCustomAnimations(0, R.anim.exit_to_left)
                    if (activeFragment != null) hide(activeFragment)
                    setCustomAnimations(R.anim.enter_from_right, 0)
                    show(exchangeTransferMoneyLinkFragment)
                }.commit()

                (exchangeTransferMoneyLinkFragment as VTFragment).initView()
            }
        }
    }

    /**
     * Function that returns the fragment using a given tag
     */
    private fun getFragmentByTag(tag: String): Fragment? {
        return when (tag) {
            walletOverviewFragmentTag -> walletOverviewFragment
            identityFragmentTag -> identityFragment
            exchangeFragmentTag -> exchangeFragment
            contactsFragmentTag -> contactsFragment
            settingsFragmentTag -> settingsFragment
            exchangeTransferMoneyLinkFragmentTag -> exchangeTransferMoneyLinkFragment
            contactChatFragmentTag -> fragmentManager.findFragmentByTag(tag)
            else -> null
        }
    }

    /**
     * Function that closes all currently opened dialogs
     */
    fun closeAllDialogs() {
        fragmentManager.fragments.filterIsInstance<DialogFragment>().forEach { fragment ->
            fragment.dismissAllowingStateLoss()
        }
    }

    /**
     * Get dialog by tag (if exists)
     */
    fun getDialogFragment(tag: String): DialogFragment? {
        return fragmentManager.findFragmentByTag(tag) as DialogFragment?
    }

    /**
     * Function that determines the height of the status bar for placement of the snackbar
     */
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId < 0) {
            result = (25.0f * resources.displayMetrics.density + 0.5F).toInt()
        } else if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * Function that displays a toast
     */
    fun displayToast(
        context: Context,
        text: String,
        isShort: Boolean = true
    ) {
        val duration = if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        Toast.makeText(context, text, duration).show()
    }

    /**
     * Function that displays a custom snackbar at the top
     */
    fun displaySnackbar(
        context: Context,
        text: String,
        view: View = window.decorView.rootView,
        type: String = SNACKBAR_TYPE_SUCCESS,
        isShort: Boolean = true,
        title: String? = null,
        actionText: String? = null,
        action: View.OnClickListener? = null,
        icon: Drawable? = null,
    ) {
        val snackbar = TSnackbar.make(view, text, if (isShort) TSnackbar.LENGTH_SHORT else TSnackbar.LENGTH_LONG)
        val snackbarView = snackbar.view

        // Add on click listener action
        val actionButton = snackbarView.findViewById<Button>(R.id.snackbar_action)
        if (actionText != null) {
            actionButton.text = actionText
        } else {
            actionButton.isVisible = false
        }
        snackbarView.setOnClickListener(action)

        // Set extra title if exists
        if (title != null) {
            snackbar.setText(HtmlCompat.fromHtml("<b>$title</b><br>\n$text", HtmlCompat.FROM_HTML_MODE_LEGACY))
        }

        // Show snackbar just below status bar
        val layoutParams = (snackbarView.layoutParams as FrameLayout.LayoutParams)
        val margin = 12.dpToPixels(context)
        val marginTop = 6.dpToPixels(context) + getStatusBarHeight()
        layoutParams.setMargins(margin, marginTop, margin, layoutParams.bottomMargin)

        // Add text and icon to snackbar
        val textView = snackbarView.findViewById<TextView>(com.androidadvance.topsnackbar.R.id.snackbar_text)
        textView.textSize = resources.getDimension(R.dimen.actionBarSubTitleSize) / resources.displayMetrics.scaledDensity

        if (icon != null) {
            textView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
            textView.compoundDrawablePadding = (10 * resources.displayMetrics.density).toInt()
        }
        textView.setTextColor(Color.WHITE)
        snackbar.setActionTextColor(Color.WHITE)

        // Set background of snackbar depending on content
        when (type) {
            SNACKBAR_TYPE_INFO -> snackbarView.background = ContextCompat.getDrawable(context, R.drawable.square_rounded_info)
            SNACKBAR_TYPE_SUCCESS -> snackbarView.background = ContextCompat.getDrawable(context, R.drawable.square_rounded_green)
            SNACKBAR_TYPE_WARNING -> snackbarView.background = ContextCompat.getDrawable(context, R.drawable.square_rounded_orange)
            SNACKBAR_TYPE_ERROR -> snackbarView.background = ContextCompat.getDrawable(context, R.drawable.square_rounded_red)
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
     * Enable custom actionbar to be aligned in center (fragment titles) and left (chat)
     */
    fun setActionBarWithGravity(alignment: Int) {
        val params = ActionBar.LayoutParams(
            ActionBar.LayoutParams.WRAP_CONTENT,
            ActionBar.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = alignment
        }

        supportActionBar?.customView = null
        supportActionBar?.setCustomView(customActionBar, params)
    }

    /**
     * Returns the custom action bar
     */
    fun getCustomActionBar(): View {
        return customActionBar
    }

    /**
     * Change title or subtitle of action bar
     */
    fun setActionBarTitle(title: String?, subtitle: String?) = with(customActionBar) {
        findViewById<TextView>(R.id.tv_actionbar_title).text = title
        findViewById<TextView>(R.id.tv_actionbar_subtitle).apply {
            text = subtitle ?: ""
            isVisible = subtitle != null
        }
    }

    /**
     * Change only subtitle of action bar
     */
    fun setActionBarSubTitle(subtitle: String) = with(customActionBar) {
        findViewById<TextView>(R.id.tv_actionbar_subtitle).apply {
            text = subtitle
            isVisible = true
        }
    }

    fun setActionBarTitleSize(size: Float) = with(customActionBar) {
        findViewById<TextView>(R.id.tv_actionbar_title).textSize = size
    }

    /**
     * Set an icon next to the action bar title
     */
    fun setActionBarTitleIcon(icon: Int? = null, color: Int? = null) = with(customActionBar) {
        findViewById<TextView>(R.id.tv_actionbar_title).apply {
            setCompoundDrawablesWithIntrinsicBounds(0, 0, icon ?: 0, 0)

            if (color != null) {
                compoundDrawables.forEach {
                    if (it != null) {
                        it.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
                    }
                }
            }
        }
    }

    /**
     * Set an icon next to the action bar subtitle
     */
    fun setActionBarSubTitleIcon(icon: Int? = null) = with(customActionBar) {
        findViewById<TextView>(R.id.tv_actionbar_subtitle)
            .setCompoundDrawablesRelativeWithIntrinsicBounds(icon ?: 0, 0, 0, 0)
    }

    /**
     * Toggle visibility of back button in action bar
     */
    fun toggleActionBar(state: Boolean) {
        supportActionBar!!.setDisplayHomeAsUpEnabled(state)
        supportActionBar!!.setHomeButtonEnabled(state)
    }

    /**
     * Toggle visibility of bottom navigation view
     */
    fun toggleBottomNavigation(state: Boolean) {
        bottomNavigationView.isVisible = state
    }

    /**
     * Enable general accessibility for the QRScanController
     */
    fun getQRScanController(): QRScanController {
        return qrScanController
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
     * Create a callback on receipt of a message within the peerchat community for notifications and contact status handling (archived, muted, blocked)
     */
    private fun onMessageCallback(
        community: PeerChatCommunity,
        peer: Peer,
        chatMessage: ChatMessage
    ) {
        // Discard callback when there's no identity imported or when the message is already in the database (delivered using ip or bluetooth packet)
        if (lifecycle.currentState == Lifecycle.State.DESTROYED || !getCommunity<IdentityCommunity>()!!.hasIdentity() || community.getDatabase().getMessageById(chatMessage.id) != null) {
            return
        }

        val peerChatStore: PeerChatStore = getStore()!!
        val currentContactState = peerChatStore.getContactState(chatMessage.sender)

        // Don't allow messages to be received while the contact is blocked, but acknowledge to stop resending
        if (currentContactState != null && currentContactState.isBlocked) {
            community.sendAck(peer, chatMessage.id)
            return
        }

        var contactImageRequest = false

        // Detect changes in local and received identityInfo, and update if necessary
        chatMessage.identityInfo?.let { identityInfo ->
            val contactImage = peerChatStore.getContactImage(peer.publicKey)
            when {
                currentContactState?.identityInfo == null -> {
                    peerChatStore.setIdentityState(chatMessage.sender, identityInfo)
                    contactImageRequest = true
                }
                currentContactState.identityInfo != identityInfo -> {
                    if (currentContactState.identityInfo!!.imageHash != identityInfo.imageHash) {
                        contactImageRequest = true
                    }

                    peerChatStore.setIdentityState(
                        currentContactState.publicKey,
                        identityInfo
                    )
                }
                contactImage == null && (identityInfo.imageHash != null && identityInfo.imageHash != "") -> {
                    contactImageRequest = true
                }
                identityInfo.imageHash == null && contactImage != null -> {
                    peerChatStore.removeContactImage(peer.publicKey)
                }
            }
        }

        val idInfo = currentContactState?.identityInfo
        val messageIdInfo = chatMessage.identityInfo
        var identityIsUpdated = idInfo?.isVerified != messageIdInfo?.isVerified || idInfo?.initials != messageIdInfo?.initials || idInfo?.surname != messageIdInfo?.surname
        var newMessages: MutableList<ChatMessage> = mutableListOf()

        // When a message is sent but not received, and this activity hasn't been initialized,
        // the message may be sent without identity info. This tells the user that the info is unknown
        if (chatMessage.identityInfo == null) {
            ChatMessage(
                UUID.randomUUID().toString(),
                resources.getString(R.string.text_identity_undetermined),
                MessageAttachment(MessageAttachment.TYPE_IDENTITY_UPDATED, 0, byteArrayOf()),
                peer.publicKey,
                getCommunity<TrustChainCommunity>()!!.myPeer.publicKey,
                false,
                chatMessage.timestamp,
                ack = true,
                read = true,
                attachmentFetched = true,
                transactionHash = null,
            ).let {
                newMessages.add(it)
            }
            identityIsUpdated = false
        }

        // Let know when the identity information of the contact has been updated
        if (identityIsUpdated) {
            val messages: MutableList<String> = mutableListOf()

            val verificationStatusText = if (messageIdInfo?.isVerified == true) {
                resources.getString(R.string.text_verified_lower)
            } else resources.getString(R.string.text_unverified_lower)

            if (idInfo == null) {
                messages.add(
                    resources.getString(
                        R.string.text_identity_updated_new,
                        verificationStatusText,
                        "${messageIdInfo?.initials} ${messageIdInfo?.surname}"
                    )
                )

                if (messageIdInfo?.isVerified != true) messages.add(resources.getString(R.string.text_identity_updated_unverified_message))
                messages.add(resources.getString(R.string.text_identity_updated_careful))
            } else {
                messages.add(resources.getString(R.string.text_identity_updated))

                when {
                    idInfo.isVerified != messageIdInfo?.isVerified -> messages.add(
                        resources.getString(
                            R.string.text_identity_updated_verification,
                            verificationStatusText
                        )
                    )
                    idInfo.initials != messageIdInfo.initials ||
                        idInfo.surname != messageIdInfo.surname -> messages.add(
                        resources.getString(
                            R.string.text_identity_updated_name,
                            "${idInfo.initials} ${idInfo.surname}",
                            "${messageIdInfo.initials} ${messageIdInfo.surname}"
                        )
                    )
                }

                if (messageIdInfo?.isVerified != true) messages.add(resources.getString(R.string.text_identity_updated_unverified_message))
                messages.add(resources.getString(R.string.text_identity_updated_careful))
            }

            val updatedIdentityChatMessage = ChatMessage(
                UUID.randomUUID().toString(),
                messages.joinToString(" "),
                MessageAttachment(
                    MessageAttachment.TYPE_IDENTITY_UPDATED,
                    0,
                    byteArrayOf()
                ),
                peer.publicKey,
                getCommunity<TrustChainCommunity>()!!.myPeer.publicKey,
                false,
                chatMessage.timestamp,
                ack = true,
                read = true,
                attachmentFetched = true,
                transactionHash = null,
            )
            newMessages.add(updatedIdentityChatMessage)
        }

        newMessages.add(chatMessage)
        newMessages.forEach { message ->
            try {
                community.getDatabase().addMessage(message)
            } catch (e: SQLiteConstraintException) {
                e.printStackTrace()
            }
        }

        // Check whether a new contact image should be requested
        if (contactImageRequest) {
            community.sendContactImageRequest(chatMessage.sender)
        }

        // Only show notification when contact is not archived or muted (and thus blocked)
        if (currentContactState == null || !(currentContactState.isArchived || currentContactState.isMuted)) {
            notificationHandler.notify(peer, chatMessage, isAppInForeground)
        }

        // Acknowledge the received chatmessage
        community.sendAck(peer, chatMessage.id)

        // Request attachment
        if (chatMessage.attachment != null) {
            when (chatMessage.attachment!!.type) {
                MessageAttachment.TYPE_IDENTITY_ATTRIBUTE -> return
                MessageAttachment.TYPE_CONTACT -> return
                MessageAttachment.TYPE_LOCATION -> return
                MessageAttachment.TYPE_TRANSFER_REQUEST -> return
                MessageAttachment.TYPE_IDENTITY_UPDATED -> return
                else -> { // for image or file
                    val fileID = chatMessage.attachment!!.content.toHex()
                    val file = MessageAttachment.getFile(this, fileID)
                    if (file.exists()) {
                        community.getDatabase().setAttachmentFetched(fileID)
                    } else {
                        community.sendAttachmentRequest(peer, fileID)
                    }
                }
            }
        }
    }

    /**
     * Callback on receipt of a request to send contact image
     */
    private fun onContactImageRequestCallback(community: PeerChatCommunity, peer: Peer) {
        val identityFaceImage = appPreferences.getIdentityFace()
        val identityFaceHash = appPreferences.getIdentityFaceHash()
        val decodedImage = identityFaceImage?.let { decodeBytes(it) }
        var bitmap = decodedImage?.let { bytesToImage(it) }

        // Try to compress the image to width of at most 200 to increase transfer speed
        if (bitmap != null) {
            bitmap = bitmap.resize(200f)
        }

        val contactImage = ContactImage(
            getCommunity<TrustChainCommunity>()!!.myPeer.publicKey,
            identityFaceHash,
            bitmap
        )

        community.sendContactImage(peer, contactImage)
    }

    /**
     * Callback on receipt of a requested contact image
     */
    private fun onContactImageCallback(contactImage: ContactImage) {
        Log.d("VTLOG", "Contact image received from ${contactImage.publicKey}")

        val peerChatStore: PeerChatStore = getStore()!!

        if (contactImage.imageHash != null && contactImage.image != null) {
            imageBytes(contactImage.image!!)?.let { imageBytes ->
                peerChatStore.setContactImage(
                    contactImage.publicKey,
                    imageBytes,
                    contactImage.imageHash!!
                )
                peerChatStore.setState(contactImage.publicKey, PeerChatStore.STATUS_IMAGE_HASH, false, contactImage.imageHash!!)
            }
        } else {
            peerChatStore.removeContactImage(contactImage.publicKey)
            peerChatStore.setState(contactImage.publicKey, PeerChatStore.STATUS_IMAGE_HASH, false, null)
        }
    }

    /**
     * On receipt of a chunk of the requested attestation execute the following
     */
    private fun attestationChunkCallback(peer: Peer, i: Int) {
        Log.i("VTLOG", "Received attestation chunk $i from ${peer.mid}.")
        Handler(Looper.getMainLooper()).post {
            displayToast(applicationContext, "Received $i attestation chunks from ${peer.mid}.")
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
                displayToast(applicationContext, "Successfully sent attestation for $attributeName to peer ${forPeer.mid}", isShort = false)
            }
        } else {
            Log.i("VTLOG", "Received attestation for attribute $attributeName with metadata: $metaData.")
            Handler(Looper.getMainLooper()).post {
                displayToast(applicationContext, "Received Attestation for $attributeName")
            }
        }
    }

    /**
     * On receipt of an attestation request (initiated by the other party) execute the following
     */
    private fun attestationRequestCallback(peer: Peer, attributeName: String, metadata: String): ByteArray {

        val parsedMetadata = JSONObject(metadata)
        val idFormat = parsedMetadata.optString("id_format", ID_METADATA)

        closeAllDialogs()

        val input = BlockingDialogManager.getInstance()
            .showAndWait<String?>(this, IdentityAttestationConfirmDialog(attributeName, idFormat, this))
            ?: throw RuntimeException("User cancelled dialog.")

        Log.i("VTLOG", "Signing attestation with value $input with format $idFormat.")

        Handler(Looper.getMainLooper()).post {
            displayToast(applicationContext, "Signing attestation for $attributeName for peer ${peer.mid} ...", isShort = false)
        }

        return when (idFormat) {
            "id_metadata_range_18plus" -> byteArrayOf(input.toByte())
            else -> input.toByteArray()
        }
    }

    /**
     * Check camera permissions
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkCameraPermissions() {
        if ((ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_CAMERA
            )
        }
    }

    /**
     * Process permission result
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("VTLOG", "CAMERA PERMISSION GRANTED")
                displayToast(
                    applicationContext,
                    resources.getString(R.string.snackbar_permission_camera_granted_other)
                )
            } else {
                Log.d("VTLOG", "CAMERA PERMISSION NOT GRANTED")
                displayToast(
                    applicationContext,
                    resources.getString(R.string.snackbar_permission_denied)
                )
                finish()
            }
        }
    }

    companion object {
        const val walletOverviewFragmentTag = "wallet_overview_fragment"
        const val identityFragmentTag = "identity_fragment"
        const val exchangeFragmentTag = "exchange_fragment"
        const val contactsFragmentTag = "contacts_fragment"
        const val qrScanControllerTag = "qrscancontroller"
        const val contactChatFragmentTag = "contact_chat_fragment"
        const val settingsFragmentTag = "settings_fragment"
        const val exchangeTransferMoneyLinkFragmentTag = "exchange_transfer_money_link_fragment"
        const val SNACKBAR_TYPE_SUCCESS = "success"
        const val SNACKBAR_TYPE_WARNING = "warning"
        const val SNACKBAR_TYPE_ERROR = "error"
        const val SNACKBAR_TYPE_INFO = "info"
        const val ARG_PUBLIC_KEY = "public_key"
        const val ARG_NAME = "name"
        const val ARG_PARENT = "parent_tag"
        const val ARG_FRAGMENT = "fragment"
        const val NOTIFICATION_INTENT_CHAT = 1
        const val NOTIFICATION_INTENT_TRANSACTION = 2
        const val PERMISSION_CAMERA = 2
    }
}
