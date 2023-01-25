@file:Suppress("DEPRECATION") // TODO: Use new method of handing intent results.

package nl.tudelft.trustchain.common.util

import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.zxing.integration.android.IntentIntegrator

/**
 * Class for returning an activity result to a fragment
 */
class FragmentIntentIntegrator(private val fragment: Fragment) :
    IntentIntegrator(fragment.activity) {
    override fun startActivityForResult(intent: Intent, code: Int) {
        fragment.startActivityForResult(intent, code)
    }
}
