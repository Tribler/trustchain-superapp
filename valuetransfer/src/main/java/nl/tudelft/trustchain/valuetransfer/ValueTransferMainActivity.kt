package nl.tudelft.trustchain.valuetransfer

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.androidadvance.topsnackbar.TSnackbar
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.jaredrummler.blockingdialog.BlockingDialogManager
import kotlinx.android.synthetic.main.main_activity_vt.*
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
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.valuetransfer.ui.VTFragment
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactChatFragment
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactsFragment
import nl.tudelft.trustchain.valuetransfer.ui.exchange.ExchangeFragment
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityFragment
import nl.tudelft.trustchain.valuetransfer.ui.settings.AppPreferences
import nl.tudelft.trustchain.valuetransfer.ui.settings.NotificationHandler
import nl.tudelft.trustchain.valuetransfer.ui.settings.SettingsFragment
import nl.tudelft.trustchain.valuetransfer.ui.walletoverview.WalletOverviewFragment
import nl.tudelft.trustchain.valuetransfer.util.dpToPixels
import nl.tudelft.trustchain.valuetransfer.util.getColorIDFromThemeAttribute
import nl.tudelft.trustchain.valuetransfer.passport.PassportHandler
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
    private val qrScanController = QRScanController()

    private lateinit var customActionBar: View
    private lateinit var notificationHandler: NotificationHandler
    private lateinit var appPreferences: AppPreferences
    private lateinit var passportHandler: PassportHandler

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

        /**
         * Create identity database tables if not exist
         */
        val identityCommunity = getCommunity<IdentityCommunity>()!!
        identityCommunity.createIdentitiesTable()
        identityCommunity.createAttributesTable()

        /**
         * Initialize notification and passport handler
         */
        notificationHandler = NotificationHandler.getInstance(this)
        passportHandler = PassportHandler.getInstance(this)

        /**
         * On initialisation of activity pre-load all fragments to allow instant switching to increase performance
         */
        fragmentManager.beginTransaction()
            .add(R.id.container, identityFragment, identityFragmentTag).hide(identityFragment)
            .add(R.id.container, exchangeFragment, exchangeFragmentTag).hide(exchangeFragment)
            .add(R.id.container, contactsFragment, contactsFragmentTag).hide(contactsFragment)
            .add(R.id.container, qrScanController, qrScanControllerTag).hide(qrScanController)
            .add(R.id.container, settingsFragment, settingsFragmentTag).hide(settingsFragment)
            .add(R.id.container, walletOverviewFragment, walletOverviewFragmentTag)
            .commit()

        fragmentManager.executePendingTransactions()

        /**
         * Bottom navigation view listener and set icon of qr scan controller the height without title
         */
        bottomNavigationViewListeners()

        val bottomView: BottomNavigationMenuView = bottomNavigationView.getChildAt(0) as BottomNavigationMenuView
        (bottomView.getChildAt(2) as BottomNavigationItemView).setIconSize(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 40f,
                resources.displayMetrics
            ).toInt()
        )

        /**
         * PeerChat community callbacks when a message is received and create archive and muted chat database tables
         */
        val peerChatCommunity = getCommunity<PeerChatCommunity>()!!
        peerChatCommunity.setOnMessageCallback(::onMessageCallback)
        peerChatCommunity.setOnContactImageRequestCallback(::onContactImageRequestCallback)
        peerChatCommunity.setOnContactImageCallback(::onContactImageCallback)
        peerChatCommunity.createContactStateTable()
        peerChatCommunity.createContactImageTable()

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

        /**
         * Enable click on notification when app is currently not on foreground
         */
        if (intent != null && getCommunity<IdentityCommunity>()!!.hasIdentity() && !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            onNewIntent(intent)
        }

        checkCameraPermissions()
    }

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
     * Enable NFC scanning
     */
    override fun onResume() {
        super.onResume()

        if (passportHandler.getNFCAdapter() != null) {
            val intent = Intent(applicationContext, this.javaClass)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val filter = arrayOf(arrayOf("android.nfc.tech.IsoDep"))
            passportHandler.getNFCAdapter()!!.enableForegroundDispatch(this, pendingIntent, null, filter)
        }
    }

    /**
     * Only allow NFC scanning when the app is on the foreground
     */
    override fun onPause() {
        super.onPause()

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
                Handler().postDelayed({
                    notificationIntentController(intent)
                }, 1000)
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
                    getActiveFragment().let { previousFragment ->
                        if (previousFragment is ContactChatFragment){
                            previousFragment.onBackPressed()
                        } else if (previousFragment is SettingsFragment) {
                            previousFragment.onBackPressed(false)
                        }
                    }

                    fragmentManager.executePendingTransactions()

                    if (fragmentTag == settingsFragmentTag) {
                        detailFragment(settingsFragmentTag, Bundle())
                    } else {
                        selectBottomNavigationItem(fragmentTag)
                        (getFragmentByTag(fragmentTag)!! as VTFragment).initView()
                    }
                }
            }
        }
    }

    private fun getActiveFragment(): Fragment {
        return fragmentManager.fragments.first {
            it.isVisible
        }
    }

    private fun notificationChatIntent(intent: Intent): Fragment {
        val publicKeyString = intent.extras?.getString(ARG_PUBLIC_KEY)

        return if (publicKeyString != null) {
            try {
                val publicKey = defaultCryptoProvider.keyFromPublicBin(publicKeyString.hexToBytes())
                val contact = getStore<ContactStore>()!!.getContactFromPublicKey(publicKey)

                ContactChatFragment().apply {
                    arguments = bundleOf(
                        ARG_PUBLIC_KEY to publicKeyString,
                        ARG_NAME to (contact?.name ?: resources.getString(R.string.text_unknown_contact)),
                        ARG_PARENT to getActiveFragment().tag
                    )
                }
            } catch(e: Exception) {
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
            val previousTag = getActiveFragment().tag

            when (menuItem.itemId) {
                R.id.walletOverviewFragment -> if (previousTag != walletOverviewFragmentTag) switchFragment(walletOverviewFragment)
                R.id.identityFragment -> if (previousTag != identityFragmentTag) switchFragment(identityFragment)
                R.id.exchangeFragment -> if (previousTag != exchangeFragmentTag) switchFragment(exchangeFragment)
                R.id.contactsFragment -> if (previousTag != contactsFragmentTag) switchFragment(contactsFragment)
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
        val previousFragment = getActiveFragment()

        fragmentManager.beginTransaction().apply {
            hide(previousFragment)
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            show(fragment)
        }.commit()

        (fragment as VTFragment).initView()
    }

    /**
     * Controller from fragment to detail view fragment
     */
    fun detailFragment(tag: String, args: Bundle) {
        val previousFragment = getActiveFragment()

        when (tag) {
            contactChatFragmentTag -> {
                val contactChatFragment = ContactChatFragment()
                contactChatFragment.arguments = args

                fragmentManager.beginTransaction().apply {
                    setCustomAnimations(0, R.anim.exit_to_left)
                    hide(previousFragment)
                    setCustomAnimations(R.anim.enter_from_right, 0)
                    add(R.id.container, contactChatFragment, contactChatFragmentTag)
                }.commit()
            }
            settingsFragmentTag -> {
                fragmentManager.beginTransaction().apply {
                    setCustomAnimations(0, R.anim.exit_to_left)
                    hide(previousFragment)
                    setCustomAnimations(R.anim.enter_from_right, 0)
                    show(settingsFragment)
                }.commit()

                (settingsFragment as VTFragment).initView()
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
     * Function that displays a snackbar at the top in the requested view with the requested text, type and length
     */
    @Suppress("UNUSED_PARAMETER")
    fun displaySnackbar(
        context: Context,
        text: String,
        view: View = window.decorView.rootView,
        type: String = SNACKBAR_TYPE_SUCCESS,
        isShort: Boolean = true,
        extraPadding: Boolean = false
    ) {
        val snackbar = TSnackbar.make(view, text, if (isShort) TSnackbar.LENGTH_SHORT else TSnackbar.LENGTH_LONG)
        val snackbarView = snackbar.view

        val layoutParams = (snackbarView.layoutParams as FrameLayout.LayoutParams)
        val margin = 12.dpToPixels(context)
        val marginTop = 6.dpToPixels(context) + getStatusBarHeight()

        layoutParams.setMargins(margin, marginTop, margin, layoutParams.bottomMargin)

        snackbar.setActionTextColor(Color.WHITE)

        when (type) {
            SNACKBAR_TYPE_SUCCESS -> snackbarView.background = ContextCompat.getDrawable(context, R.drawable.square_rounded_dark_green)
            SNACKBAR_TYPE_WARNING -> snackbarView.background = ContextCompat.getDrawable(context, R.drawable.square_rounded_orange)
            SNACKBAR_TYPE_ERROR -> snackbarView.background = ContextCompat.getDrawable(context, R.drawable.square_rounded_red)
        }

        val textView = snackbarView.findViewById<TextView>(com.androidadvance.topsnackbar.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)

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
    fun setActionBarTitleIcon(icon: Int? = null) = with(customActionBar) {
        findViewById<TextView>(R.id.tv_actionbar_title).setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            icon ?: 0,
            0
        )
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
    private fun onMessageCallback(community: PeerChatCommunity, peer: Peer, chatMessage: ChatMessage) {
        // Discard callback when there's no identity or when the message is already in the database (delivered using ip or bluetooth packet)
        if (!getCommunity<IdentityCommunity>()!!.hasIdentity() || community.getDatabase().getMessageById(chatMessage.id) != null) {
            return
        }

        Log.d("VTLOG", "MESSAGE RECEIVED FROM $peer with ${chatMessage.message}")

        val peerChatStore: PeerChatStore = getStore()!!
        val contactState = peerChatStore.getContactState(chatMessage.sender)

        // Don't allow messages to be received while the contact is blocked, but acknowledge to stop resending
        if (contactState != null && contactState.isBlocked) {
            community.sendAck(peer, chatMessage.id)
            return
        }

        var contactImageRequest = false

        chatMessage.identityInfo?.let { identityInfo ->

            when {
                contactState?.identityInfo == null -> {
                    peerChatStore.setIdentityState(chatMessage.sender, identityInfo)
                    contactImageRequest = true
                }
                contactState.identityInfo != identityInfo -> {
                    Log.d("VTLOG", "CONTACT STATE IS NOT UP2DATE")
                    if (contactState.identityInfo!!.imageHash != identityInfo.imageHash) {
                        contactImageRequest = true
                    }
                    peerChatStore.setIdentityState(
                        contactState.publicKey,
                        contactState.identityInfo!!
                    )
                }
            }

//            if (contactState != null) {
//                if (contactState.identityInfo == null) {
//                    peerChatStore.setIdentityState(chatMessage.sender, identityInfo)
//                    contactImageRequest = true
//                } else {
//                    if (contactState.identityInfo!! != identityInfo) {
//                        Log.d("VTLOG", "CONTACT STATE IS NOT UP2DATE")
//                        if (contactState.identityInfo!!.imageHash != identityInfo.imageHash) {
//                            contactImageRequest = true
//                        }
//                        peerChatStore.setIdentityState(
//                            contactState.publicKey,
//                            contactState.identityInfo!!
//                        )
//                    } else {
//                        Log.d("VTLOG", "CONTACT STATE IS UP2DATE")
//                    }
////                    if (contactState.identityInfo!!.isVerified != identityInfo.isVerified) {
////                        peerChatStore.setState(chatMessage.sender, PeerChatStore.STATUS_VERIFICATION, identityInfo.isVerified)
////                    }
////                    if (contactState.identityInfo!!.imageHash != identityInfo.imageHash) {
////                        peerChatStore.setState(chatMessage.sender, PeerChatStore.STATUS_IMAGE_HASH, false, identityInfo.imageHash)
////                        contactImageRequest = true
////                    }
////                    if (contactState.identityInfo!!.initials != identityInfo.initials) {
////                        Log.d("VTLOG", "TRIGGERED INITIALS")
////                        peerChatStore.setState(chatMessage.sender, PeerChatStore.STATUS_INITIALS, false, identityInfo.initials)
////                    }
////                    if (contactState.identityInfo!!.surname != identityInfo.surname) {
////                        peerChatStore.setState(chatMessage.sender, PeerChatStore.STATUS_SURNAME, false, identityInfo.surname)
////                    }
////                    if (contactState.identityInfo!!.imageHash != peerChatStore.getContactImageHash(chatMessage.sender)) {
////                        contactImageRequest = true
////                    }
//                }
//            } else {
//                peerChatStore.setIdentityState(chatMessage.sender, identityInfo)
//                contactImageRequest = true
//            }
        }

        if (contactImageRequest) {
            Log.d("VTLOG", "SEND CONTACT IMAGE REQUEST UPON RECEIPT OF MESSAGE TO ${chatMessage.sender}")
            community.sendContactImageRequest(chatMessage.sender)
        }

        try {
            community.getDatabase().addMessage(chatMessage)
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        }

        // Only show notification when contact is not archived or muted (and thus blocked)
        if (contactState == null || (!contactState.isArchived && !contactState.isMuted)) {
            notificationHandler.notify(peer, chatMessage)
        }

        community.sendAck(peer, chatMessage.id)

        // Request attachment
        if (chatMessage.attachment != null) {
            when (chatMessage.attachment!!.type) {
                MessageAttachment.TYPE_IMAGE -> return
                MessageAttachment.TYPE_IDENTITY_ATTRIBUTE -> return
                MessageAttachment.TYPE_CONTACT -> return
                MessageAttachment.TYPE_LOCATION -> return
                MessageAttachment.TYPE_TRANSFER_REQUEST -> return
                else -> community.sendAttachmentRequest(
                    peer,
                    chatMessage.attachment!!.content.toHex()
                )
            }
        }
    }

    /**
     * Callback on receipt of a request to send contact image
     */
    private fun onContactImageRequestCallback(community: PeerChatCommunity, peer: Peer) {
        Log.d("VTLOG", "CONTACT IMAGE REQUEST RECEIVED FROM $peer")

        val identityFaceImage = appPreferences.getIdentityFace()
        val identityFaceHash = appPreferences.getIdentityFaceHash()
        val decodedImage = identityFaceImage?.let { decodeBytes(it) }
//        val decodedImage = identityFaceImage?.let { ContactImage.decodeImage(it) }
        var bitmap = decodedImage?.let { bytesToImage(it) }
//        var bitmap = if (decodedImage != null) ContactImage.bytesToImage(decodedImage) else null
        Log.d("VTLOG", "BITMAP NULL: ${bitmap == null}")

        // Try to compress the image to width of at most 200 to increase transfer speed
        if (bitmap != null) {
            bitmap = bitmap.resize(200f)
//            try {
//                val width = bitmap.width
//                val height = bitmap.height
//                val scale = if (width > 200) 200f / width else 1.0f
//                val matrix = Matrix().apply {
//                    postScale(scale, scale)
//                }
//                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
//                Log.d("VTLOG", "BITMAP WIDTH: ${bitmap.width}")
//                Log.d("VTLOG", "BITMAP HEIGHT: ${bitmap.height}")
//                Log.d("VTLOG", "BITMAP SIZE IS: ${bitmap?.byteCount}")
//            } catch(e: Exception) {
//                e.printStackTrace()
//                Log.d("VTLOG", "FAILED TO RESIZE BITMAP")
//            }
        }
        Log.d("VTLOG", "CREATING CONTACT IMAGE OBJECT")
        val contactImage = ContactImage(
            getCommunity<TrustChainCommunity>()!!.myPeer.publicKey,
            identityFaceHash,
            bitmap
        )
        Log.d("VTLOG", "CREATED CONTACT IMAGE OBJECT: ${contactImage}")

        Log.d("VTLOG", "CONTACT IMAGE SEND TO $peer $contactImage")

        community.sendContactImage(peer, contactImage)
    }

    /**
     * Callback on receipt of a requested contact image
     */
    private fun onContactImageCallback(community: PeerChatCommunity, contactImage: ContactImage) {
        Log.d("VTLOG", "CONTACT IMAGE RECEIVED FROM ${contactImage.publicKey}")
        Log.d("VTLOG", "PUBLIC KEY: ${contactImage.publicKey.keyToBin().toHex()}")
        Log.d("VTLOG", "IMAGE HASH: ${contactImage.imageHash}")
        Log.d("VTLOG", "IMAGE NULL: ${contactImage.image == null}")

        if (contactImage.imageHash != null && contactImage.image != null) {

//           ContactImage.imageToBytes(contactImage.image!!)?.let { imageBytes ->
            imageBytes(contactImage.image!!)?.let { imageBytes ->
                community.getDatabase().setContactImage(
                    contactImage.publicKey,
                    imageBytes,
                    contactImage.imageHash!!
                )
                community.getDatabase().setState(contactImage.publicKey, PeerChatStore.STATUS_IMAGE_HASH, false, contactImage.imageHash!!)
            }
        } else {
            community.getDatabase().removeContactImage(contactImage.publicKey)
            community.getDatabase().setState(contactImage.publicKey, PeerChatStore.STATUS_IMAGE_HASH, false, null)
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

        val parsedMetadata = JSONObject(metadata)
        val idFormat = parsedMetadata.optString("id_format", ID_METADATA)

        closeAllDialogs()

        val input = BlockingDialogManager.getInstance()
            .showAndWait<String?>(this, IdentityAttestationConfirmDialog(attributeName, idFormat, this))
            ?: throw RuntimeException("User cancelled dialog.")

        Log.i("VTLOG", "Signing attestation with value $input with format $idFormat.")

        Handler(Looper.getMainLooper()).post {
            displaySnackbar(applicationContext, "Signing attestation for $attributeName for peer ${peer.mid} ...", isShort = false)
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
        if (requestCode == ContactChatFragment.PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED}) {
                Log.d("VTLOG", "CAMERA PERMISSION GRANTED")
                displaySnackbar(
                    applicationContext,
                    resources.getString(R.string.snackbar_permission_camera_granted_other)
                )
            } else {
                Log.d("VTLOG", "CAMERA PERMISSION NOT GRANTED")
                displaySnackbar(
                    applicationContext,
                    resources.getString(R.string.snackbar_permission_denied),
                    type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
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

        const val SNACKBAR_TYPE_SUCCESS = "success"
        const val SNACKBAR_TYPE_WARNING = "warning"
        const val SNACKBAR_TYPE_ERROR = "error"

        const val ARG_PUBLIC_KEY = "public_key"
        const val ARG_NAME = "name"
        const val ARG_PARENT = "parent_tag"
        const val ARG_FRAGMENT = "fragment"

        const val NOTIFICATION_INTENT_CHAT = 1
        const val NOTIFICATION_INTENT_TRANSACTION = 2

        const val PERMISSION_LOCATION = 1
        const val PERMISSION_CAMERA = 2
    }
}
