package nl.tudelft.trustchain.valuetransfer.passport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import net.sf.scuba.data.Gender
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.passport.mlkit.CameraSource
import nl.tudelft.trustchain.valuetransfer.passport.mlkit.CameraSourcePreview
import nl.tudelft.trustchain.valuetransfer.passport.mlkit.GraphicOverlay
import nl.tudelft.trustchain.valuetransfer.passport.mlkit.TextRecognitionProcessor
import org.jmrtd.lds.icao.MRZInfo
import java.io.IOException
import java.lang.Exception

open class PassportCaptureActivity : Activity(), TextRecognitionProcessor.ResultListener {

    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var feedbackText: TextView? = null
    private var documentType: String = PassportHandler.DOCUMENT_TYPE_OTHER
    private var nfcSupported = false
    private var overrideScan = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.capture_activity)

        Log.d("VTLOG", "CAPTURE STARTED")

        if (intent.hasExtra(PassportHandler.DOCUMENT_TYPE)) {
            documentType = intent.getStringExtra(PassportHandler.DOCUMENT_TYPE) ?: PassportHandler.DOCUMENT_TYPE_OTHER
            Log.d("VTLOG", "DOCUMENT TYPE: $documentType")
        }

        if (intent.hasExtra(PassportHandler.NFC_SUPPORTED)) {
            nfcSupported = intent.getBooleanExtra(PassportHandler.NFC_SUPPORTED, false)
        }

        preview = findViewById(R.id.camera_source_preview)
        graphicOverlay = findViewById(R.id.graphics_overlay)
        feedbackText = findViewById(R.id.tvFeedback)

        findViewById<Button>(R.id.btnScanPrevious).setOnClickListener {
            onBackPressed()
        }

        // Override the scanning
        findViewById<TextView>(R.id.tvScanTitle).setOnClickListener {
            overrideScan += 1
            if (overrideScan % 5 == 0) {
                onSuccess(MRZInfo(
                    "P",
                    "NLD",
                    "Test",
                    "Dummy",
                    "AA12345678",
                    "NLD",
                    "990101",
                    Gender.MALE,
                    "221212",
                    "123456789"
                ))
            }
        }

        onResume()
    }

    override fun onResume() {
        super.onResume()
        Log.d("VTLOG", "CAPTURE RESUME")
        createCameraSource()
        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        preview?.stop()
    }

    private fun createCameraSource() {
        if (cameraSource == null) {
            cameraSource =
                CameraSource(
                    this,
                    graphicOverlay,
                    feedbackText
                )
            cameraSource!!.setFacing(CameraSource.CAMERA_FACING_BACK)
        }

        cameraSource!!.setMachineLearningFrameProcessor(
            TextRecognitionProcessor(
                documentType,
                nfcSupported,
                this,
                this@PassportCaptureActivity
            )
        )
        Log.d("VTLOG", "CAMERA SOURCE CREATED")
    }

    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                preview?.start(cameraSource, graphicOverlay, feedbackText)

                Log.d("VTLOG", "CAMERA SOURCE STARTED")
            } catch (e: IOException) {
                Log.d("VTLOG", "ERROR: UNABLE TO START CAMERA SOURCE", e)
                cameraSource!!.release()
                cameraSource = null
            }
        } else {
            Log.d("VTLOG", "CAMERA SOURCE IS NULL")
        }
    }

    override fun onSuccess(mrzInfo: MRZInfo?) {
        Log.d("VTLOG", "CAPTURE SUCCEEDED")

        val returnIntent = Intent()
        returnIntent.putExtra(PassportHandler.MRZ_RESULT, mrzInfo)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    override fun onError(exp: Exception?) {
        Log.d("VTLOG", "CAPTURE FAILED")
        setResult(RESULT_CANCELED)
        finish()
    }
}
