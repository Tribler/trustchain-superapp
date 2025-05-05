package nl.tudelft.trustchain.eurotoken.ui

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.nfc.NfcError
import java.io.IOException
import android.nfc.TagLostException
import android.view.View
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.eurotoken.databinding.ActivityNfcReaderBinding
import java.util.*
import nl.tudelft.trustchain.eurotoken.nfc.EuroTokenHCEService

//TODO check if we can delete nfchandler, nfcState, and nfcViewModel
// not for now

//activity handles reading dtaa from nfc HCE service
// on other device -> uses NFC reader mode
//apdu exchange
class NfcReaderActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var binding: ActivityNfcReaderBinding

    companion object {
        private const val TAG = "NfcReader"
        private const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        private const val CONNECTION_TIMEOUT_MS = 5000


        //extra
//        const val EXTRA_NFC_DATA = "nl.tudelft.trustchain.eurotoken.NFC_DATA"
//        const val EXTRA_NFC_STATUS = "nl.tudelft.trustchain.eurotoken.NFC_STATUS"
//        const val EXTRA_NFC_ERROR = "nl.tudelft.trustchain.eurotoken.NFC_ERROR"

        // AID
        // this is what HCE service has to respond to
        // must match apduservice.xml!!
        // ISO/IEC 7816-4 -> 5-16 bytes, start with 0xF2
        private const val HCE_GOAL_AID = "F222222222"

        // select app via AID
        // follows ISO/IEC 7816-4 (AID) and ISO/IEC 7816-3 (SELECT)
        // CLA | INS | P1 | P2 | Lc | Data
        val CMD_SELECT_AID: ByteArray = byteArrayOf(
            0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
            HCE_GOAL_AID.length.div(2).toByte() // Lc field = AID length
        ) + HCE_GOAL_AID.hexToBytes()

        // Command to request the data after successful selection
        val CMD_READ_DATA: ByteArray = byteArrayOf(
            0x00.toByte(), 0xB0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
        )

        // Status words
        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00.toByte()) //success
        // private val SW_CONDITIONS_NOT_SATISFIED = byteArrayOf(0x69, 0x85.toByte())
        // private val SW_UNKNOWN_ERROR = byteArrayOf(0x6F.toByte(), 0x00.toByte())
        // private val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00.toByte())
        val SW_CONDITIONS_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x85.toByte())
        private val SW_CMD_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00.toByte())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNfcReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            updateStatus("NFC is not available on this device.")
            finishWithError(NfcError.NFC_UNSUPPORTED)
            return
        }
        if (!nfcAdapter!!.isEnabled) {
            updateStatus("NFC is disabled. Please enable it.")
            finishWithError(NfcError.NFC_DISABLED)
            return
        }
        updateStatus(getString(R.string.waiting_for_nfc_tap))
        binding.tvReaderResult.visibility = View.INVISIBLE
    }

    override fun onResume() {
        super.onResume()
        // enable reader mode when Activity is resumed
        // when the tag is discovered --> this will be invoked
        nfcAdapter?.enableReaderMode(this, this, READER_FLAGS, null)
        Log.d(TAG, "Reader Mode enabled.")
    }

    override fun onPause() {
        //TODO check if sufficient!
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
        Log.d(TAG, "Reader Mode disabled.")
    }


    // nfc tag is found that matches the rquired flags --> NFC-A
    override fun onTagDiscovered(tag: Tag?) {
        Log.i(TAG, "NFC Tag Discovered: $tag")
        // here we try to get the isodep interface
        // ISO 7816-4 APDUs
        // first check if supports isodep
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            lifecycleScope.launch {
                commWithTag(isoDep)
            }
        } else {
            Log.w(TAG, "Tag does not support IsoDep communication.")
            runOnUiThread { updateStatus("Incompatible NFC tag found.") }
            // Maybe finish with error or just wait for a compatible tag
        }
    }
    private suspend fun commWithTag(isoDep: IsoDep) {
        // only one worker for interactions --> app needds to stay smooth
        // its necessary to launch a dispatchers.io thread
        // isodep -> |connect | |transceive| |close| are blocking
        // must not run on main thread
        try {
            withContext(Dispatchers.IO) {
                isoDep.connect()
                isoDep.timeout = 5000

                runOnUiThread { updateStatus("Tag connected. Selecting App...") }
                Log.d(TAG, "Sending SELECT AID: ${CMD_SELECT_AID.toHex()}")
                val selectResult = isoDep.transceive(CMD_SELECT_AID)
                Log.d(TAG, "SELECT AID response: ${selectResult.toHex()}")

                if (!checkSuccess(selectResult)) {
                    Log.e(TAG, "SELECT AID failed. Status: ${selectResult.getStatusString()}")
                    runOnUiThread { updateStatus("Failed to select App on tag.") }
                    finishWithError(NfcError.AID_SELECT_FAILED)
                    return@withContext
                }

                Log.i(TAG, "AID Selected successfully.")
                runOnUiThread { updateStatus("App selected. Reading confirmation...") }

                Log.d(TAG, "Sending READ DATA: ${CMD_READ_DATA.toHex()}")
                val readResult = isoDep.transceive(CMD_READ_DATA)
                Log.d(TAG, "READ DATA response: ${readResult.toHex()}")

                if (checkSuccess(readResult)) {
                    val payloadBytes = readResult.copyOfRange(0, readResult.size - 2)
                    val payloadString = String(payloadBytes, Charsets.UTF_8)
                    Log.i(TAG, "Data read successfully: $payloadString")

                    // is the received payload the static value? -->TODO change
                    val confirmationStatus: String
                    if (payloadString == EuroTokenHCEService.NFC_TEST_TXT) {
                        confirmationStatus = "NFC Confirmation OK!"
                        Log.i(TAG, confirmationStatus)
                    } else {
                        confirmationStatus = "Received unexpected data: $payloadString"
                        Log.w(TAG, confirmationStatus)
                    }

                    runOnUiThread {
                        updateStatus("Data received.")
                        updateResult(confirmationStatus)
                    }
                    finishWithSuccess(payloadString)
                } else {
                    val statusString = readResult.getStatusString()
                    Log.e(TAG, "READ DATA failed. Status: $statusString")
                    val error = when {
                        Arrays.equals(readResult, SW_CONDITIONS_NOT_SATISFIED) -> NfcError.HCE_DATA_NOT_READY
                        else -> NfcError.READ_FAILED
                    }
                    runOnUiThread { updateStatus("Failed to read data. Status: $statusString") }
                    finishWithError(error)
                }
            }
        } catch (e: TagLostException) {
            Log.e(TAG, "Tag lost during communication.", e)
            runOnUiThread { updateStatus("NFC tag moved away too quickly.") }
            finishWithError(NfcError.TAG_LOST)
        } catch (e: IOException) {
            Log.e(TAG, "IOException during NFC communication.", e)
            runOnUiThread { updateStatus("Communication error. Try again.") }
            finishWithError(NfcError.IO_ERROR)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during NFC communication.", e)
            runOnUiThread { updateStatus("An unexpected error occurred.") }
            finishWithError(NfcError.UNKNOWN_ERROR)
        } finally {
            try {
                isoDep.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing IsoDep.", e)
            }
        }
    }

    // helpers 

    private fun finishWithSuccess(data: String) {
        val resultIntent = Intent()
        resultIntent.putExtra("nl.tudelft.trustchain.eurotoken.NFC_DATA", data)
        resultIntent.putExtra("nl.tudelft.trustchain.eurotoken.NFC_STATUS", "SUCCESS")
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun finishWithError(error: NfcError) {
        val resultIntent = Intent()
        resultIntent.putExtra("nl.tudelft.trustchain.eurotoken.NFC_STATUS", "ERROR")
        resultIntent.putExtra("nl.tudelft.trustchain.eurotoken.NFC_ERROR", error.name)
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

    private fun updateStatus(message: String) {
        // not on main thread
        runOnUiThread {
            binding.tvReaderStatus.text = message
        }
    }

    private fun updateResult(result: String) {
        runOnUiThread {
            binding.tvReaderResult.text = result
            binding.tvReaderResult.visibility = View.VISIBLE
            //binding.tvReaderResult.visibility = View.VISIBLE
        }
    }

    private fun checkSuccess(response: ByteArray?): Boolean {
        return response != null && response.size >= 2 &&
            response[response.size - 2] == SW_OK[0] &&
            response[response.size - 1] == SW_OK[1]
    }

    private fun ByteArray.getStatusString(): String {
        return if (this.size >= 2) {
            this.copyOfRange(this.size - 2, this.size).toHex()
        } else {
            "Invalid response"
        }
    }

    private fun String.hexToBytes(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }.uppercase()



}
