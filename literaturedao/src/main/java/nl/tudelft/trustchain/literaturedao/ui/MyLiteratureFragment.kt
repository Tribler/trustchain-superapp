package nl.tudelft.trustchain.literaturedao.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.literaturedao.R

class MyLiteratureFragment : BaseFragment(R.layout.fragment_literature_overview) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun filePicker(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        @Suppress("DEPRECATION") // TODO: Fix deprecation issue.
        startActivityForResult(intent, 100)
    }
}
