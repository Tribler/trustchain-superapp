package nl.tudelft.trustchain.ssi.ui.verifier

import org.json.JSONObject

class VerificationHelper() {

    var presentation = hashMapOf<Long, JSONObject>()

    companion object {
        private lateinit var instance: VerificationHelper

        fun getInstance(): VerificationHelper {
            return if (!this::instance.isInitialized) {
                this.instance = VerificationHelper()
                this.instance
            } else {
                this.instance
            }
        }
    }
}
