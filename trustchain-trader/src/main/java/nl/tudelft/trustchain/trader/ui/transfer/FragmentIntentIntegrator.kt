package nl.tudelft.trustchain.trader.ui.transfer

import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.zxing.integration.android.IntentIntegrator

/**
 * Class for returning an activity result to a fragment
 */
class FragmentIntentIntegrator(private val fragment: Fragment) :
    IntentIntegrator(fragment.activity) {
    @Suppress("DEPRECATION")
    override fun startActivityForResult(intent: Intent, code: Int) {
        fragment.startActivityForResult(intent, code)
    }
}
