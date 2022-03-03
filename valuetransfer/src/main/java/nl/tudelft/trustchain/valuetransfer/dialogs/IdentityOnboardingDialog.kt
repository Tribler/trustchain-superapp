package nl.tudelft.trustchain.valuetransfer.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import nl.tudelft.trustchain.common.valuetransfer.extensions.encodeImage
import nl.tudelft.trustchain.common.valuetransfer.extensions.exitEnterView
import nl.tudelft.trustchain.common.valuetransfer.extensions.viewFadeIn
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.*
import nl.tudelft.trustchain.valuetransfer.passport.PassportHandler
import nl.tudelft.trustchain.valuetransfer.passport.entity.EDocument
import org.jmrtd.lds.icao.*
import java.text.SimpleDateFormat
import java.util.*

class IdentityOnboardingDialog : VTDialogFragment(), View.OnClickListener {
    private lateinit var startView: ConstraintLayout
    private lateinit var startImportingButton: Button
    private lateinit var startOpenNFCSettingsButton: Button

    private lateinit var scanView: ConstraintLayout
    private lateinit var scanPassportSelected: ConstraintLayout
    private lateinit var scanIDCardSelected: ConstraintLayout

    private lateinit var readView: ConstraintLayout
    private lateinit var readContinueButton: Button
    private lateinit var readAgainButton: Button
    private lateinit var readingStartImage: ImageView
    private lateinit var readingFinishedSuccessImage: ImageView
    private lateinit var readingFinishedErrorImage: ImageView
    private lateinit var readingSpinner: ProgressBar
    private lateinit var readingText: TextView
    private lateinit var readingSuccessText: TextView
    private lateinit var readingFailedText: TextView

    private lateinit var confirmView: ConstraintLayout
    private lateinit var confirmScanAgainButton: Button

    private var nfcSupported = false
    private var nfcEnabled = false
    private var nfcSwitchCount = 0

    private var eDocument: EDocument? = null

    private lateinit var bottomSheetDialog: Dialog
    private lateinit var dialogView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            bottomSheetDialog = Dialog(requireContext(), R.style.FullscreenDialog)

            @Suppress("DEPRECATION")
            bottomSheetDialog.window?.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

            val view = layoutInflater.inflate(R.layout.dialog_identity_onboarding, null)
            dialogView = view

            // Dialog cannot be discarded on outside touch
            bottomSheetDialog.setCancelable(false)
            bottomSheetDialog.setCanceledOnTouchOutside(false)

            // Start View
            startView = view.findViewById(R.id.clStartView)
            startImportingButton = view.findViewById(R.id.btnStartImporting)
            startImportingButton.setOnClickListener(this)
            startOpenNFCSettingsButton = view.findViewById(R.id.btnOpenNFCSettings)
            startOpenNFCSettingsButton.setOnClickListener(this)
            view.findViewById<TextView>(R.id.tvStepTwo).setOnClickListener(this)
            view.findViewById<Button>(R.id.btnStartCancel).setOnClickListener(this)

            // Scan Document View
            scanView = view.findViewById(R.id.clScanView)
            view.findViewById<LinearLayout>(R.id.llScanSelectPassport).setOnClickListener(this)
            scanPassportSelected = view.findViewById(R.id.clScanPassportSelected)
            view.findViewById<LinearLayout>(R.id.llScanSelectIDCard).setOnClickListener(this)
            scanIDCardSelected = view.findViewById(R.id.clScanIDCardSelected)
            view.findViewById<Button>(R.id.btnScanCancel).setOnClickListener(this)

            // Read Document View
            readView = view.findViewById(R.id.clReadView)
            readingStartImage = view.findViewById(R.id.ivReadDocumentIcon)
            readingFinishedSuccessImage = view.findViewById(R.id.ivReadingFinishedIcon)
            readingFinishedErrorImage = view.findViewById(R.id.ivReadingFinishedIconError)
            readingSpinner = view.findViewById(R.id.pbReading)

            readingText = view.findViewById(R.id.tvReading)
            readingSuccessText = view.findViewById(R.id.tvReadingSuccess)
            readingFailedText = view.findViewById(R.id.tvReadingFailed)

            readAgainButton = view.findViewById(R.id.btnReadAgain)
            readAgainButton.setOnClickListener(this)
            readContinueButton = view.findViewById(R.id.btnReadContinue)
            readContinueButton.setOnClickListener(this)
            view.findViewById<Button>(R.id.btnReadCancel).setOnClickListener(this)
            view.findViewById<Button>(R.id.btnReadPrevious).setOnClickListener(this)

            // Confirm Document View
            confirmView = view.findViewById(R.id.clConfirmView)
            confirmScanAgainButton = view.findViewById(R.id.btnConfirmScanAgain)
            confirmScanAgainButton.setOnClickListener(this)
            view.findViewById<Button>(R.id.btnConfirm).setOnClickListener(this)
            view.findViewById<Button>(R.id.btnConfirmCancel).setOnClickListener(this)

            // Check device status of NFC
            getNFCDeviceStatus()

            // Set passport handler to init state
            passportHandler.init()

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    override fun onResume() {
        super.onResume()

        if (startView.isVisible) {
            Log.d("VTLOG", "NFC STATUS CHECK")
            getNFCDeviceStatus()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnStartImporting -> {
                initScanView()
                startView.exitEnterView(requireContext(), scanView)
            }
            R.id.tvStepTwo -> {
                nfcSwitchCount += 1
                if (nfcSwitchCount % 5 == 0) {
                    nfcSupported = !nfcSupported
                    getNFCDeviceStatus(true)
                }
            }
            R.id.btnStartCancel -> dismissDialog()
            R.id.llScanSelectPassport -> startPassportScan(PassportHandler.DOCUMENT_TYPE_PASSPORT)
            R.id.llScanSelectIDCard -> startPassportScan(PassportHandler.DOCUMENT_TYPE_ID_CARD)
            R.id.btnScanCancel -> dismissDialog()
            R.id.btnReadContinue -> {
                confirmView()
                readView.exitEnterView(requireContext(), confirmView)
            }
            R.id.btnReadAgain -> readGoBack()
            R.id.btnReadCancel -> dismissDialog()
            R.id.btnReadPrevious -> readGoBack()
            R.id.btnConfirmScanAgain -> {
                initScanView()
                confirmView.exitEnterView(requireContext(), scanView, false)
            }
            R.id.btnConfirm -> confirmIdentity()
            R.id.btnConfirmCancel -> dismissDialog()
        }
    }

    /**
     * Fetch NFC device status and apply to views
     */
    private fun getNFCDeviceStatus(override: Boolean = false) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(parentActivity)
        if (!override) {
            nfcSupported = nfcAdapter != null
        }
        nfcEnabled = nfcSupported && nfcAdapter?.isEnabled == true

        dialogView.findViewById<LinearLayout>(R.id.llNFCSupported).isVisible = nfcSupported
        dialogView.findViewById<LinearLayout>(R.id.llNFCNotSupported).isVisible = !nfcSupported
        dialogView.findViewById<FrameLayout>(R.id.flNFCEnabled).isVisible = nfcSupported

        confirmScanAgainButton.isVisible = !nfcSupported
        dialogView.findViewById<TextView>(R.id.tvConfirmUnsupportedText).isVisible = !nfcSupported
        dialogView.findViewById<RelativeLayout>(R.id.rlStatusVerified).isVisible = nfcSupported
        dialogView.findViewById<RelativeLayout>(R.id.rlStatusNotVerified).isVisible = !nfcSupported

        if (nfcSupported) {
            dialogView.findViewById<LinearLayout>(R.id.llNFCEnabled).isVisible = nfcEnabled
            dialogView.findViewById<LinearLayout>(R.id.llNFCNotEnabled).isVisible = !nfcEnabled
            startOpenNFCSettingsButton.apply {
                isVisible = !nfcEnabled

                setOnClickListener {
                    openNFCSettings()
                }
            }
        }

        startImportingButton.isVisible = if (nfcSupported) nfcEnabled else true
    }

    /**
     * Open device settings to enable NFC
     */
    private fun openNFCSettings() {
        val intent = Intent().apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    action = Settings.ACTION_NFC_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, parentActivity.packageName)
                }
                else -> {
                    action = "android.settings.WIRELESS_SETTINGS"
                    putExtra("app_package", parentActivity.packageName)
                    putExtra("app_uid", parentActivity.applicationInfo.uid)
                }
            }
        }

        startActivity(intent)
    }

    /**
     * Dismiss dialog to initial state
     */
    private fun dismissDialog() {
        passportHandler.deactivateNFCAdapter()
        passportHandler.unsubscribe()
        dialog?.dismiss()
    }

    /**
     * Start the scan of the document, based on selected document
     */
    private fun startPassportScan(type: String) {
        passportHandler.setDocumentType(type)

        when (type) {
            PassportHandler.DOCUMENT_TYPE_PASSPORT -> scanPassportSelected.viewFadeIn(requireContext(), 500)
            PassportHandler.DOCUMENT_TYPE_ID_CARD -> scanIDCardSelected.viewFadeIn(requireContext(), 500)
        }

        @Suppress("DEPRECATION")
        Handler().postDelayed({ passportHandler.startPassportScanActivity(this, nfcSupported) }, 500)

        @Suppress("DEPRECATION")
        Handler().postDelayed({ initScanView() }, 1000)
    }

    /**
     * Launch NFC scan as async task
     */
    private fun launchNFCScan() = lifecycleScope.executeAsyncTask(
        onPreExecute = {
            Log.d("VTLOG", "LAUNCHED NFC SCAN")
            startReadingView()
        },
        doInBackground = {
            passportHandler.startNFCReader()
        },
        onPostExecute = { result ->
            if (result is EDocument && result.personDetails != null && result.documentType != null) {
                this.eDocument = result

                endReadingView(true)

                readingFinishedSuccessImage.viewFadeIn(requireContext(), 1000)

                passportHandler.deactivateNFCAdapter()
                passportHandler.unsubscribe()
            } else {
                readingFailedText.text = if (result is String) result else resources.getString(R.string.text_error_passport)
                endReadingView(false)
            }
        }
    )

    /**
     * Create and confirm identity
     */
    private fun confirmIdentity() {
        if (eDocument == null || eDocument!!.personDetails == null) return

        val identity = try {
            getIdentityCommunity().createIdentity(
                eDocument!!.personDetails!!.name.toString(),
                eDocument!!.personDetails!!.surname.toString(),
                "",
                eDocument!!.personDetails!!.dateOfBirth!!.toLong(),
                eDocument!!.personDetails!!.nationality.toString(),
                eDocument!!.personDetails!!.gender!!.substring(0, 1),
                eDocument!!.personDetails!!.personalNumber!!.toLong(),
                eDocument!!.personDetails!!.serialNumber.toString(),
                nfcSupported,
                eDocument!!.personDetails!!.dateOfExpiry ?: 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("VTLOG", "IDENTITY CREATION FAILED")

            parentActivity.displayToast(
                requireContext(),
                resources.getString(R.string.snackbar_identity_create_error),
            )

            return
        }

        if (getIdentityCommunity().hasIdentity()) {
            ConfirmDialog(
                resources.getString(R.string.text_confirm_replace_identity)
            ) {
                try {
                    getIdentityCommunity().deleteIdentity()

                    getAttestationCommunity().database.getAllAttestations().forEach {
                        getAttestationCommunity().database.deleteAttestationByHash(it.attestationHash)
                    }

                    appPreferences.deleteIdentityFace()

                    getIdentityCommunity().addIdentity(identity)
                    eDocument!!.personDetails!!.faceImage?.let { bitmap ->
                        parentActivity.appPreferences().setIdentityFace(encodeImage(bitmap))
                    }
                    bottomSheetDialog.dismiss()

                    parentActivity.reloadActivity()
                } catch (e: Exception) {
                    e.printStackTrace()
                    parentActivity.displayToast(
                        requireContext(),
                        resources.getString(R.string.snackbar_identity_remove_error)
                    )
                }
            }.show(parentFragmentManager, tag)
        } else {
            getIdentityCommunity().addIdentity(identity)
            eDocument!!.personDetails!!.faceImage?.let { bitmap ->
                parentActivity.appPreferences().setIdentityFace(encodeImage(bitmap))
            }
            bottomSheetDialog.dismiss()

            parentActivity.reloadActivity()
        }
    }

    /**
     * Init scan view
     */
    private fun initScanView() {
        scanPassportSelected.isVisible = false
        scanIDCardSelected.isVisible = false
    }

    /**
     * Init reading view
     */
    private fun initReadingView() {
        eDocument = null

        readingStartImage.isVisible = true
        readingSpinner.isVisible = false
        readingFinishedSuccessImage.isVisible = false
        readingFinishedErrorImage.isVisible = false

        readingText.isVisible = false
        readingSuccessText.isVisible = false
        readingFailedText.isVisible = false
        readingFailedText.text = ""

        readAgainButton.isVisible = false
        readContinueButton.isVisible = false
    }

    /**
     * Change reading view after it started
     */
    private fun startReadingView() {
        readingStartImage.isVisible = false
        readingSpinner.isVisible = true
        readingFinishedSuccessImage.isVisible = false
        readingFinishedErrorImage.isVisible = false

        readingText.isVisible = true
        readingSuccessText.isVisible = false
        readingFailedText.isVisible = false
        readingFailedText.text = ""

        readAgainButton.isVisible = false
        readContinueButton.isVisible = false
    }

    /**
     * Change reading view after it ended reading
     */
    private fun endReadingView(success: Boolean) {
        readingStartImage.isVisible = false
        readingSpinner.isVisible = false
        readingFinishedSuccessImage.isVisible = success
        readingFinishedErrorImage.isVisible = !success

        readingText.isVisible = false
        readingSuccessText.isVisible = success
        readingFailedText.isVisible = !success

        readAgainButton.isVisible = !success
        readContinueButton.isVisible = success
    }

    /**
     * Return from read view to scan view
     */
    private fun readGoBack() {
        passportHandler.init()
        initScanView()
        readView.exitEnterView(requireContext(), scanView, false)
    }

    /**
     * Init confirm view
     */
    private fun confirmView() {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)

        eDocument.let { eDoc ->
            if (eDoc!!.personDetails == null || eDoc.documentType == null) return

            eDoc.personDetails?.let { pDetails ->
                dialogView.findViewById<CardView>(R.id.cvConfirmIdentityImage).isVisible = pDetails.faceImage != null
                dialogView.findViewById<ImageView>(R.id.ivIdentityFaceImage).setImageBitmap(pDetails.faceImage)

                dialogView.findViewById<TextView>(R.id.tvIDFullName).text = StringBuilder()
                    .append(pDetails.name)
                    .append(" ")
                    .append(pDetails.surname)
                dialogView.findViewById<TextView>(R.id.tvIDPersonalNumber).text = pDetails.personalNumber
                dialogView.findViewById<TextView>(R.id.tvIDGender).text = pDetails.gender
                dialogView.findViewById<TextView>(R.id.tvIDDateOfBirth).text = dateFormat.format(pDetails.dateOfBirth)
                dialogView.findViewById<TextView>(R.id.tvIDDateOfExpiry).text = dateFormat.format(pDetails.dateOfExpiry)
                dialogView.findViewById<TextView>(R.id.tvIDDocumentNumber).text = pDetails.serialNumber
                dialogView.findViewById<TextView>(R.id.tvIDNationality).text = pDetails.nationality
                dialogView.findViewById<TextView>(R.id.tvIDIssuer).text = pDetails.issuerAuthority
            }
        }
    }

    /**
     * Receive listener from NFC reader
     */
    override fun onReceive(type: String, data: Any?) {
        Log.d("VTLOG", "ON RECEIVE $type")
        when (type) {
            RECEIVE_TYPE_NFC -> launchNFCScan()
        }
    }

    /**
     * Passport capture result is received
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            (data.getSerializableExtra(PassportHandler.MRZ_RESULT) as MRZInfo).run {
                if (nfcSupported) {
                    passportHandler.activateNFCAdapter()
                    Log.d("VTLOG", "NFC ADAPTER IS ACTIVATED: ${passportHandler.getNFCAdapter() != null}")
                    Log.d("VTLOG", "ISO DEP IS NULL: ${passportHandler.getISODep() == null}")
                    passportHandler.subscribe(this@IdentityOnboardingDialog)

                    passportHandler.setBasicAuthenticationKey(
                        documentNumber,
                        dateOfBirth,
                        dateOfExpiry
                    )
                    initReadingView()
                    scanView.exitEnterView(requireContext(), readView)
                } else {
                    val personDetails = passportHandler.mrzToPersonDetails(this)
                    val eDocument = EDocument(
                        passportHandler.getDocumentType(),
                        personDetails
                    )

                    this@IdentityOnboardingDialog.eDocument = eDocument

                    confirmView()
                    scanView.exitEnterView(requireContext(), confirmView)
                }
            }
        }
    }
}
