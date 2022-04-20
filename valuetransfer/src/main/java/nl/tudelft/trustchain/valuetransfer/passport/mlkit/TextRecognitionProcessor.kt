package nl.tudelft.trustchain.valuetransfer.passport.mlkit

import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.util.Log
import android.widget.TextView
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import net.sf.scuba.data.Gender
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.passport.PassportHandler
import nl.tudelft.trustchain.valuetransfer.passport.mlkit.GraphicOverlay.Graphic
import org.jmrtd.lds.icao.MRZInfo
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import java.util.regex.Pattern

class TextRecognitionProcessor(
    private val documentType: String,
    private val nfcSupported: Boolean,
    private val resultListener: ResultListener,
    private val activity: Activity
) {
    private var textRecognizer: TextRecognizer = TextRecognition.getClient()
    private lateinit var scannedTextBuffer: String
    private val shouldThrottle = AtomicBoolean(false)

    fun stop() { textRecognizer.close() }

    @Throws(MlKitException::class)
    fun process(
        data: ByteBuffer,
        frameMetadata: FrameMetadata,
        graphicOverlay: GraphicOverlay,
        feedbackText: TextView
    ) {
        if (shouldThrottle.get()) {
            return
        }
        val inputImage = InputImage.fromByteBuffer(
            data,
            frameMetadata.width,
            frameMetadata.height,
            frameMetadata.rotation,
            InputImage.IMAGE_FORMAT_NV21
        )
        detectInVisionImage(inputImage, frameMetadata, graphicOverlay, feedbackText)
    }

    private fun detectInImage(image: InputImage): Task<Text> {
        return textRecognizer.process(image)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onSuccess(
        results: Text,
        frameMetadata: FrameMetadata,
        graphicOverlay: GraphicOverlay,
        feedbackText: TextView
    ) {
        graphicOverlay.clear()
        feedbackText.text = null
        scannedTextBuffer = ""

        val blocks = results.textBlocks
        for (i in blocks.indices) {
            val lines = blocks[i].lines
            for (j in lines.indices) {
                val elements = lines[j].elements
                for (k in elements.indices) {
                    filterScannedText(graphicOverlay, feedbackText, elements[k])
                }
            }
        }
    }

    private fun onFailure(e: java.lang.Exception) {
        Log.w("PPACT", "Text detection failed.$e")
        resultListener.onError(e)
    }

    private fun parseMRZStringForType(type: String, lines: List<String>): String? {
        return when (type) {
            TYPE_DOCUMENT_CODE -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[0].substring(0, 1)
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[0].substring(0, 1)
                }
                else -> null
            }
            TYPE_DOCUMENT_NUMBER -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[0].substring(5, 14).replace("O", "0")
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[1].substring(0, 9).replace("O", "0")
                }
                else -> null
            }
            TYPE_PERSONAL_NUMBER -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[0].substring(15, 24).replace("O", "0").replace("<", "").trim()
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[1].substring(28, 37).replace("O", "0").replace("<", "")
                }
                else -> null
            }
            TYPE_DATE_OF_BIRTH -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[1].substring(0, 6)
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[1].substring(13, 19)
                }
                else -> null
            }
            TYPE_DATE_OF_EXPIRY -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[1].substring(8, 14)
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[1].substring(21, 27)
                }
                else -> null
            }
            TYPE_ISSUER -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[0].substring(2, 5)
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[0].substring(2, 5)
                }
                else -> null
            }
            TYPE_NATIONALITY -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[1].substring(15, 18)
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[1].substring(10, 13)
                }
                else -> null
            }
            TYPE_GENDER -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[1].substring(7, 8)
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[1].substring(20, 21)
                }
                else -> null
            }
            TYPE_GIVEN_NAMES -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[2].split("<<")[1].replace("<", " ").trim()
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[0].substring(5).split("<<")[1].replace("<", " ").trim()
                }
                else -> null
            }
            TYPE_SURNAME -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[2].split("<<")[0].replace("<", " ")
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[0].substring(5).split("<<")[0].replace("<", " ")
                }
                else -> null
            }
            TYPE_COMBINED -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> lines[0].substring(5, 30).replace("O", "0") + lines[1].substring(0, 7) + lines[1].substring(8, 15) + lines[1].substring(18, 29)
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> lines[1].substring(0, 10).replace("O", "0") + lines[1].substring(13, 20) + lines[1].substring(21, 43)
                else -> null
            }
            TYPE_CHECK_DIGIT_DOCUMENT_NUMBER -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[0][14].toString()
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[1][9].toString()
                }
                else -> null
            }
            TYPE_CHECK_DIGIT_DATE_OF_BIRTH -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[1][6].toString()
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[1][19].toString()
                }
                else -> null
            }
            TYPE_CHECK_DIGIT_DATE_OF_EXPIRY -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[1][14].toString()
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[1][27].toString()
                }
                else -> null
            }
            TYPE_CHECK_DIGIT_PERSONAL_NUMBER -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[0][29].toString()
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[1][42].toString()
                }
                else -> null
            }
            TYPE_CHECK_DIGIT_COMBINED -> when (documentType) {
                PassportHandler.DOCUMENT_TYPE_ID_CARD -> {
                    lines[1][29].toString()
                }
                PassportHandler.DOCUMENT_TYPE_PASSPORT -> {
                    lines[1][43].toString()
                }
                else -> null
            }
            else -> null
        }
    }

    private fun filterScannedText(
        graphicOverlay: GraphicOverlay,
        feedbackText: TextView,
        element: Text.Element
    ) {
        val textGraphic: Graphic = TextGraphic(graphicOverlay, element, Color.GREEN)
        scannedTextBuffer += element.text

        Log.d("VTLOG", "TEST SCANNED TEXT $documentType")

        val matchers: Map<String, List<Matcher>> = mapOf(
            PassportHandler.DOCUMENT_TYPE_ID_CARD to listOf(
                PATTERN_ID_LINE_1.matcher(scannedTextBuffer),
                PATTERN_ID_LINE_2.matcher(scannedTextBuffer),
                PATTERN_ID_LINE_3.matcher(scannedTextBuffer)
            ),
            PassportHandler.DOCUMENT_TYPE_PASSPORT to listOf(
                PATTERN_PASSPORT_LINE_1.matcher(scannedTextBuffer),
                PATTERN_PASSPORT_LINE_2.matcher(scannedTextBuffer)
            )
        )

        if (matchers[documentType]!!.all { it.find() }) {
            graphicOverlay.add(textGraphic)

            val lines = matchers[documentType]!!.map {
                it.group(0) ?: return
            }

            val checkDigits: Map<String, Int> = try {
                mapOf(
                    TYPE_DOCUMENT_NUMBER to parseMRZStringForType(
                        TYPE_CHECK_DIGIT_DOCUMENT_NUMBER,
                        lines
                    )!!.toInt(),
                    TYPE_PERSONAL_NUMBER to parseMRZStringForType(
                        TYPE_CHECK_DIGIT_PERSONAL_NUMBER,
                        lines
                    )!!.toInt(),
                    TYPE_DATE_OF_BIRTH to parseMRZStringForType(
                        TYPE_CHECK_DIGIT_DATE_OF_BIRTH,
                        lines
                    )!!.toInt(),
                    TYPE_DATE_OF_EXPIRY to parseMRZStringForType(
                        TYPE_CHECK_DIGIT_DATE_OF_EXPIRY,
                        lines
                    )!!.toInt(),
                    TYPE_COMBINED to parseMRZStringForType(
                        TYPE_CHECK_DIGIT_COMBINED,
                        lines
                    )!!.toInt(),
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("VTLOG", "CHECK DIGIT IS NOT A DIGIT")
                return
            }

            checkDigits.forEach { (s, i) ->
                Log.d("VTLOG", "CHECK DIGIT FOR $s  = $i")
            }

            val documentNumber = parseMRZStringForType(TYPE_DOCUMENT_NUMBER, lines) ?: return
            val dateOfBirth = parseMRZStringForType(TYPE_DATE_OF_BIRTH, lines) ?: return
            val dateOfExpiry = parseMRZStringForType(TYPE_DATE_OF_EXPIRY, lines) ?: return

            if (!digitValidation(documentNumber, checkDigits[TYPE_DOCUMENT_NUMBER]!!)) {
                Log.d("VTLOG", "DOCUMENT NUMBER CHECK DIGIT FAILS: ${checkDigits[TYPE_DOCUMENT_NUMBER]}")

                feedbackText.text = activity.resources.getString(R.string.text_passport_error_invalid_check_digit_doc_nr)
                return
            }
            if (!digitValidation(dateOfBirth, checkDigits[TYPE_DATE_OF_BIRTH]!!)) {
                Log.d("VTLOG", "DATE BIRTH CHECK DIGIT FAILS: ${checkDigits[TYPE_DATE_OF_BIRTH]}")
                feedbackText.text = activity.resources.getString(R.string.text_passport_error_invalid_check_digit_date_birth)
                return
            }
            if (!digitValidation(dateOfExpiry, checkDigits[TYPE_DATE_OF_EXPIRY]!!)) {
                Log.d("VTLOG", "DATE EXPIRY CHECK DIGIT FAILS: ${checkDigits[TYPE_DATE_OF_EXPIRY]}")
                feedbackText.text = activity.resources.getString(R.string.text_passport_error_invalid_check_digit_date_expiry)
                return
            }

            when (nfcSupported) {
                true -> {
                    Log.d(
                        "VTLOG",
                        "RESULT OF SCAN $documentType -> \n" +
                            "Document Number: $documentNumber \n" +
                            "Date of Birth: $dateOfBirth \n" +
                            "Date of Expiry: $dateOfExpiry"
                    )

                    buildTempMrz(
                        documentNumber,
                        dateOfBirth,
                        dateOfExpiry
                    )
                }
                else -> {
                    val documentCode = parseMRZStringForType(TYPE_DOCUMENT_CODE, lines) ?: return
                    val issuer = parseMRZStringForType(TYPE_ISSUER, lines) ?: return
                    val personalNumber =
                        parseMRZStringForType(TYPE_PERSONAL_NUMBER, lines) ?: return
                    val gender = parseMRZStringForType(TYPE_GENDER, lines) ?: return
                    val nationality = parseMRZStringForType(TYPE_NATIONALITY, lines) ?: return
                    val surname = parseMRZStringForType(TYPE_SURNAME, lines) ?: return
                    val givenNames = parseMRZStringForType(TYPE_GIVEN_NAMES, lines) ?: return
                    val combined = parseMRZStringForType(TYPE_COMBINED, lines) ?: return

                    if (!digitValidation(personalNumber, checkDigits[TYPE_PERSONAL_NUMBER]!!)) {
                        feedbackText.text = activity.resources.getString(R.string.text_passport_error_invalid_check_digit_personal_nr)
                        return
                    }
                    if (!digitValidation(combined, checkDigits[TYPE_COMBINED]!!)) {
                        feedbackText.text = activity.resources.getString(R.string.text_passport_error_invalid_check_digit_combined)
                        return
                    }

                    Log.d(
                        "VTLOG",
                        "RESULT OF SCAN $documentType ->>>> \n" +
                            " Document Code: $documentCode \n" +
                            " Given Names: $givenNames \n" +
                            " Surname: $surname \n" +
                            " Date of Birth: $dateOfBirth \n" +
                            " Date of Expiry: $dateOfExpiry \n" +
                            " Personal Number: $personalNumber \n" +
                            " Document Number: $documentNumber \n" +
                            " Gender: $gender \n" +
                            " Nationality: $nationality \n" +
                            " Issuer: $issuer"
                    )

                    buildTempMrzComplete(
                        documentCode,
                        givenNames,
                        surname,
                        dateOfBirth,
                        dateOfExpiry,
                        personalNumber,
                        documentNumber,
                        gender,
                        nationality,
                        issuer
                    )
                }
            }?.let {
                finishScanning(it)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun detectInVisionImage(
        image: InputImage,
        metadata: FrameMetadata,
        graphicOverlay: GraphicOverlay,
        feedbackText: TextView
    ) {
        detectInImage(image)
            .addOnSuccessListener { results ->
                shouldThrottle.set(false)
                this@TextRecognitionProcessor.onSuccess(results, metadata, graphicOverlay, feedbackText)
            }
            .addOnFailureListener { e ->
                shouldThrottle.set(false)
                this@TextRecognitionProcessor.onFailure(e)
            }
        // Begin throttling until this frame of input has been processed, either in onSuccess or
        // onFailure.
        shouldThrottle.set(true)
    }

    private fun finishScanning(mrzInfo: MRZInfo) {
        try {
            if (isMrzValid(mrzInfo)) {
                // Delay returning result 1 sec. in order to make mrz text become visible on graphicOverlay by user
                // You want to call 'resultListener.onSuccess(mrzInfo)' without no delay
                @Suppress("DEPRECATION")
                Handler().postDelayed({ resultListener.onSuccess(mrzInfo) }, 1000)
            }
        } catch (e: java.lang.Exception) {
            Log.d("VTLOG", "MRZ DATA is not valid")
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    private fun buildTempMrz(
        documentNumber: String,
        dateOfBirth: String,
        expiryDate: String
    ): MRZInfo? {
        return try {
            MRZInfo(
                "P",
                "NNN",
                "",
                "",
                documentNumber,
                "NNN",
                dateOfBirth,
                Gender.UNSPECIFIED,
                expiryDate,
                ""
            )
        } catch (e: java.lang.Exception) {
            Log.d("VTLOG", "MRZInfo error : " + e.localizedMessage)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun buildTempMrzComplete(
        documentCode: String,
        givenNames: String,
        surname: String,
        dateOfBirth: String,
        expiryDate: String,
        personalNumber: String,
        documentNumber: String,
        gender: String,
        nationality: String,
        issuer: String
    ): MRZInfo? {
        return try {
            var genderType = Gender.UNSPECIFIED
            if ("M".equals(gender, ignoreCase = true)) {
                genderType = Gender.MALE
            }
            if ("F".equals(gender, ignoreCase = true)) {
                genderType = Gender.FEMALE
            }
            MRZInfo(
                documentCode,
                issuer,
                surname,
                givenNames,
                documentNumber,
                nationality,
                dateOfBirth,
                genderType,
                expiryDate,
                personalNumber
            )
        } catch (e: java.lang.Exception) {
            Log.d("VTLOG", "MRZInfo error : " + e.localizedMessage)
            null
        }
    }

    private fun isMrzValid(mrzInfo: MRZInfo): Boolean {
        return mrzInfo.documentNumber != null && mrzInfo.documentNumber.length >= 8 &&
            mrzInfo.dateOfBirth != null && mrzInfo.dateOfBirth.length == 6 &&
            mrzInfo.dateOfExpiry != null && mrzInfo.dateOfExpiry.length == 6
    }

    interface ResultListener {
        fun onSuccess(mrzInfo: MRZInfo?)
        fun onError(exp: Exception?)
    }

    companion object {
        val PATTERN_ID_LINE_1 = Pattern.compile("([A|C|I][A-Z<])([A-Z<]{3})([A-Z0-9<]{9})([0-9<])([A-Z0-9<]{15})")
        val PATTERN_ID_LINE_2 = Pattern.compile("([0-9]{6})([0-9])([M|F|<])([0-9]{6})([0-9])([A-Z<]{3})([A-Z0-9<]{11})([0-9])")
        val PATTERN_ID_LINE_3 = Pattern.compile("([A-Z]{1,30})([<[A-Z]{0,30}]{0,30})(<<)([A-Z]{1,30})(<[A-Z]{0,30})(<{0,30})")
        val PATTERN_PASSPORT_LINE_1 = Pattern.compile("(P[A-Z<])([A-Z<]{3})([A-Z]{1,30})([<[A-Z]{0,30}]{0,30})(<<)([A-Z]{1,30})(<[A-Z]{0,30})(<{0,30})")
        val PATTERN_PASSPORT_LINE_2 = Pattern.compile("([A-Z0-9<]{9})([0-9]{1})([A-Z<]{3})([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z0-9<]{14})([0-9]{1})([0-9]{1})")

        const val TYPE_DOCUMENT_CODE = "document_code"
        const val TYPE_DOCUMENT_NUMBER = "document_number"
        const val TYPE_PERSONAL_NUMBER = "personal_number"
        const val TYPE_DATE_OF_BIRTH = "date_of_birth"
        const val TYPE_DATE_OF_EXPIRY = "date_of_expiry"
        const val TYPE_ISSUER = "issuer"
        const val TYPE_NATIONALITY = "nationality"
        const val TYPE_GENDER = "gender"
        const val TYPE_GIVEN_NAMES = "given_names"
        const val TYPE_SURNAME = "surname"
        const val TYPE_COMBINED = "combined"
        const val TYPE_CHECK_DIGIT_DOCUMENT_NUMBER = "check_digit_document_number"
        const val TYPE_CHECK_DIGIT_DATE_OF_BIRTH = "check_digit_date_of_birth"
        const val TYPE_CHECK_DIGIT_DATE_OF_EXPIRY = "check_digit_date_of_expiry"
        const val TYPE_CHECK_DIGIT_PERSONAL_NUMBER = "check_digit_personal_number"
        const val TYPE_CHECK_DIGIT_COMBINED = "check_digit_combined"

        fun digitValidation(string: String, checkDigit: Int): Boolean {
            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val digitWeights: Array<Int> = arrayOf(7, 3, 1)

            var sum = 0

            string.forEachIndexed { index, char ->
                val value = when {
                    char.equals("<") -> 0
                    alphabet.contains(char) -> (alphabet.indexOf(char) + 10)
                    char.isDigit() -> Character.getNumericValue(char)
                    else -> 0
                }
                sum += value * digitWeights[index % 3]
            }

            return sum % 10 == checkDigit
        }
    }
}
