package nl.tudelft.trustchain.valuetransfer.passport

import android.app.Activity
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import net.sf.scuba.smartcards.CardService
import nl.tudelft.trustchain.common.util.FragmentIntentIntegrator
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.passport.extensions.getPassportImage
import nl.tudelft.trustchain.common.valuetransfer.extensions.mrzDateToTime
import nl.tudelft.trustchain.valuetransfer.passport.entity.EDocument
import nl.tudelft.trustchain.valuetransfer.passport.entity.PersonDetails
import org.jmrtd.BACKey
import org.jmrtd.BACKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.CardSecurityFile
import org.jmrtd.lds.DataGroup
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.SecurityInfo
import org.jmrtd.lds.icao.*
import org.jmrtd.lds.iso19794.FaceImageInfo
import java.security.PublicKey

@Suppress("DEPRECATION")
class PassportHandler(
    private val activity: Activity,
) {

    private lateinit var documentType: String
    private lateinit var basicAuthenicationKey: BACKeySpec
    private var nfcAdapter: NfcAdapter? = null
    private var isoDep = MutableLiveData<IsoDep>()
    //    private var isoDep: IsoDep? = null
    private var paceSucceeded = false
    private lateinit var passportService: PassportService

    private var subscriber: Fragment? = null

//    private var documentType = DOCUMENT_TYPE_OTHER
//    private lateinit var basicAuthenicationKey: BACKeySpec
//    private var nfcAdapter: NfcAdapter? = null
//    private var isoDep = MutableLiveData<IsoDep>()
////    private var isoDep: IsoDep? = null
//    private var paceSucceeded = false
//    private lateinit var passportService: PassportService
//
//    private var subscriber: Fragment? = null

    init {
        init()
    }

    fun init() {
        documentType = DOCUMENT_TYPE_OTHER

        nfcAdapter = null
        isoDep.value = null
        paceSucceeded = false
        subscriber = null
    }

    fun subscribe(fragment: Fragment) {
        subscriber = fragment
    }

    fun unsubscribe() {
        subscriber = null
    }

    /**
     * Activate the NFC adapter
     */
    fun activateNFCAdapter() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
//        isoDep = null
        isoDep.postValue(null)
    }

    /**
     * Deactivate the NFC adapter
     */
    fun deactivateNFCAdapter() {
        nfcAdapter = null
//        isoDep = null
//        isoDep.postValue(null)
    }

    /**
     * Return the NFC Adapter
     */
    fun getNFCAdapter(): NfcAdapter? {
        return nfcAdapter
    }

    /**
     * Set the current ISODEP value
     */
    fun setIsoDep(tag: Tag) {
        Log.d("VTLOG", "SET ISODEP: $tag")
//        isoDep = IsoDep.get(tag)
//
//        Log.d("VTLOG", "ISODEP SETTED: $isoDep")
//
//        isoDep?.let {
//            if (subscriber is VTDialogFragment) {
//                (subscriber as VTDialogFragment).onReceive(
//                    VTDialogFragment.RECEIVE_TYPE_NFC,
//                    it
//                )
//            }
//        }

//        val iso = IsoDep.get(tag)
//        iso.timeout = 20000
//        isoDep.value = iso

        isoDep.value = IsoDep.get(tag)

        isoDep.value?.let {
            if (subscriber is VTDialogFragment) {
                (subscriber as VTDialogFragment).onReceive(
                    VTDialogFragment.RECEIVE_TYPE_NFC,
                    it
                )
            }
        }
    }

    fun getISODep(): IsoDep? {
        return isoDep.value
    }

    /**
     * Set the preferred document type for capture
     */
    fun setDocumentType(documentType: String) {
        this.documentType = documentType
    }

    /**
     * Get the document type for capture
     */
    fun getDocumentType(): String {
        return documentType
    }

    /**
     * Set basic authentication keys
     */
    fun setBasicAuthenticationKey(documentNumber: String, dateOfBirth: String, dateOfExpiry: String) {
        basicAuthenicationKey = BACKey(documentNumber, dateOfBirth, dateOfExpiry)
    }

    /**
     * Start capturing
     */
    fun startPassportScanActivity(fragment: Fragment, nfcSupported: Boolean) {
        run {
            val integrator = FragmentIntentIntegrator(fragment)
                .setOrientationLocked(true)
                .setBeepEnabled(false)
                .setCameraId(0)

            integrator.captureActivity = PassportCaptureActivity::class.java
            integrator.addExtra(DOCUMENT_TYPE, documentType)
            integrator.addExtra(NFC_SUPPORTED, nfcSupported)
            integrator.setRequestCode(REQUEST_CODE_PASSPORT_SCAN)
            integrator.initiateScan()
        }
    }

    /**
     * Return the passport service
     */
    private fun setPassportService(): String? {
        try {
            Log.d("VTLOG", "CARD SERVICE INITIATED")

            if (nfcAdapter == null) {
                return activity.resources.getString(R.string.text_error_passport_nfc_unavailable)
            }
            Log.d("VTLOG", "NFC ADAPTER IS ACTIVATED: ${nfcAdapter != null}")
            Log.d("VTLOG", "ISODEP VALUE IS NULL: ${isoDep.value == null}")

            val cardService = CardService.getInstance(isoDep.value)
            cardService.open()

            Log.d("VTLOG", "CARD SERVICE OPENED")

            Log.d("VTLOG", "PASSPORT SERVICE INITIATED")
            passportService = PassportService(
                cardService,
                PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                PassportService.DEFAULT_MAX_BLOCKSIZE,
                true,
                false
            )
            passportService.open()

            Log.d("VTLOG", "PASSPORT SERVICE OPENED")
        } catch(e: Exception) {
            e.printStackTrace()
            Log.d("VTLOG", "PASSPORT SERVICE ERROR: ${e.localizedMessage}")
            return activity.resources.getString(R.string.text_error_passport_service)
        }

        return null
    }

//    @Throws(IOException::class)
//    fun ensureConnected() {
////        if (isoDep == null) return
//
////        if (!isoDep!!.isConnected) {
////            isoDep!!.timeout = 10000
////        }
//        if (!isoDep.value?.isConnected!!) {
//            isoDep.value?.let {
//                val iso = it
//                iso.connect()
//                iso.timeout = 10000
//                isoDep.value = iso
//            }
//        }
//    }

    fun startNFCReader(): Any? {
        paceSucceeded = false

        val eDocument = EDocument()

        try {
            setPassportService()?.let {
                Log.d("VTLOG", "ERROR PASSPORT SERVICE")
                return it
            }

            establishNFCConnection()

            passportService.sendSelectApplet(paceSucceeded)

            basicAuthentication()?.let {
                Log.d("VTLOG", "ERROR BASIC AUTHENTICATION")
                return it
            }

            try {
                eDocument.personDetails = getPersonDetails()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("VTLOG", "ERROR PERSON DETAILS: ${e.localizedMessage}")
                return activity.resources.getString(R.string.text_error_passport_person_details)
            }

            try {
                eDocument.documentType = getMRZDocumentType()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("VTLOG", "ERROR DOCUMENT TYPE: ${e.localizedMessage}")
//                returnError("Failed to confirm card document type. Please scan again or go back and try again.")
                return activity.resources.getString(R.string.text_error_passport_document_type)
            }

//            try {
//                ensureConnected()
//                eDocument.documentPublicKey = getDocumentPublicKey()
////                docPublicKey = getDocumentPublicKey()
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.d("VTLOG", "ERROR DOCUMENT PUBLIC KEY: ${e.localizedMessage}")
//            }

            return eDocument

//            return EDocument(
//                docType,
//                personDet,
//                docPublicKey
//            )

//            return EDocument(
//                getMRZDocumentType(),
//                getPersonDetails(),
//                getDocumentPublicKey()
//            )
        } catch(e: Exception) {
            e.printStackTrace()
            Log.d("VTLOG", "ERROR NFC SCAN: ${e.localizedMessage}")
        }

        return null
    }

//    private fun returnError(error: String) {
//        if (subscriber is VTDialogFragment) {
//            (subscriber as VTDialogFragment).onError(
//                VTDialogFragment.ERROR_TYPE_NFC,
//                error
//            )
//        }
//    }

    private fun establishNFCConnection() {
        try {
            val cardSecurityFile = CardSecurityFile(
                passportService.getInputStream(PassportService.EF_CARD_SECURITY)
            )

            val securityInfoCollection: Collection<SecurityInfo> = cardSecurityFile.securityInfos

            for (securityInfo in securityInfoCollection) {
                if (securityInfo is PACEInfo) {
                    val paceInfo: PACEInfo = securityInfo
                    passportService.doPACE(
                        basicAuthenicationKey,
                        paceInfo.objectIdentifier,
                        PACEInfo.toParameterSpec(paceInfo.parameterId),
                        null
                    )
                    paceSucceeded = true
                    Log.d("VTLOG", "PACE SUCCEEDED")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("VTLOG", "ERROR PACE: ${e.localizedMessage}")
        }
    }

    private fun basicAuthentication(): String? {
        try {
            if (!paceSucceeded) {
                try {
                    passportService.getInputStream(PassportService.EF_COM).read()
                } catch (e: Exception) {
                    passportService.doBAC(basicAuthenicationKey)
                }
            }
        } catch(e: Exception) {
            e.printStackTrace()
            Log.d("VTLOG", "ERROR BAC: ${e.localizedMessage}")

            return activity.resources.getString(R.string.text_error_passport_card_authentication)
        }

        return null
    }

    fun getDataGroupFile(spec: Short): DataGroup? {
        val input = passportService.getInputStream(spec)

        return when (spec) {
            PassportService.EF_DG1 -> DG1File(input)
            PassportService.EF_DG2 -> DG2File(input)
            PassportService.EF_DG15 -> DG15File(input)
            else -> null
        }

    }

    private fun getMRZInfo(): MRZInfo {
        val dg1In = passportService.getInputStream(PassportService.EF_DG1)
        val dg1File = DG1File(dg1In)

        return dg1File.mrzInfo
    }

    fun mrzToPersonDetails(mrzInfo: MRZInfo, image: Bitmap? = null): PersonDetails {
        return PersonDetails().apply {
            name = mrzInfo.secondaryIdentifier.replace("<", " ").trim()
            surname = mrzInfo.primaryIdentifier.replace("<", " ").trim()
            personalNumber = mrzInfo.personalNumber
            gender = mrzInfo.gender.toString().substring(0, 1)
            dateOfBirth = mrzInfo.dateOfBirth.mrzDateToTime()
            dateOfExpiry = mrzInfo.dateOfExpiry.mrzDateToTime()
            serialNumber = mrzInfo.documentNumber
            nationality = mrzInfo.nationality
            issuerAuthority = mrzInfo.issuingState
            faceImage = image
        }
    }

    fun getPersonDetails(): PersonDetails {
        Log.d("VTLOG", "FETCHING PERSON DETAILS FROM MRZ")
        val mrzInfo = getMRZInfo()
        val passportImage = getPassportImage()

        return mrzToPersonDetails(mrzInfo, passportImage)

//        return PersonDetails().apply {
//            name = mrzInfo.secondaryIdentifier.replace("<", " ").trim()
//            Log.d("VTLOG", "NAME: $name")
//            surname = mrzInfo.primaryIdentifier.replace("<", " ").trim()
//            personalNumber = mrzInfo.personalNumber
//            gender = mrzInfo.gender.toString().substring(0, 1)
//            dateOfBirth = DateUtil.convertFromMrzDateToTime(mrzInfo.dateOfBirth)
//            dateOfExpiry = DateUtil.convertFromMrzDateToTime(mrzInfo.dateOfExpiry)
//            serialNumber = mrzInfo.documentNumber
//            nationality = mrzInfo.nationality
//            issuerAuthority = mrzInfo.issuingState
//            faceImage = passportImage
//        }
    }

    private fun getMRZDocumentType(): String {
        Log.d("VTLOG", "FETCHING DOCUMENT TYPE FROM MRZ")
        if (getMRZInfo().documentCode == DOCUMENT_TYPE_ID_CARD) {
            documentType = DOCUMENT_TYPE_ID_CARD
        } else if (getMRZInfo().documentCode == DOCUMENT_TYPE_PASSPORT) {
            documentType = DOCUMENT_TYPE_PASSPORT
        }

        Log.d("VTLOG", "DOC TYPE: $documentType")

        return documentType
    }

    fun getPassportImage(): Bitmap? {
        Log.d("VTLOG", "FETCHING PASSPORT IMAGE")
        val dgFile = getDataGroupFile(PassportService.EF_DG2) as DG2File

        val allFaceImageInfos = arrayListOf<FaceImageInfo>()
        dgFile.faceInfos.forEach { faceInfo ->
            allFaceImageInfos.addAll(faceInfo.faceImageInfos)
        }

        return if (allFaceImageInfos.isNotEmpty()) {
            val faceImageInfo = allFaceImageInfos.iterator().next()
            getPassportImage(faceImageInfo)
        } else null
    }

    fun getDocumentPublicKey(): PublicKey {
        val dgFile = getDataGroupFile(PassportService.EF_DG15) as DG15File
        return dgFile.publicKey
    }

    companion object {
        const val REQUEST_CODE_PASSPORT_SCAN = 101

        const val DOCUMENT_TYPE = "document_type"
        const val NFC_SUPPORTED = "nfc_supported"
        const val MRZ_RESULT = "mrz_result"

        const val DOCUMENT_TYPE_PASSPORT = "P"
        const val DOCUMENT_TYPE_ID_CARD = "I"
        const val DOCUMENT_TYPE_OTHER = "O"

        private lateinit var instance: PassportHandler
        fun getInstance(activity: Activity): PassportHandler {
            if (!::instance.isInitialized) {
                instance = PassportHandler(activity)
            }
            return instance
        }
    }
}
