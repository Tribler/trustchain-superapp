package nl.tudelft.trustchain.common.ui

import com.journeyapps.barcodescanner.CaptureActivity

/*
 * This dummy class enables the barcode scanner to function in portrait mode.
 * See: https://github.com/journeyapps/zxing-android-embedded#changing-the-orientation
 */
class QRCodeActivityPortrait : CaptureActivity() {

}
