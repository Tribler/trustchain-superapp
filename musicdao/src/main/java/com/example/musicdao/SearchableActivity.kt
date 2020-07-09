package com.example.musicdao

import android.app.Activity
import android.app.ListActivity
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.media.session.MediaButtonReceiver.handleIntent
import nl.tudelft.trustchain.common.BaseActivity

class SearchableActivity: MusicService() {
    override val navigationGraph = R.navigation.musicdao_navgraph

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.view_results)

//        val transaction = supportFragmentManager.beginTransaction()
//        transaction.add(R.id.baseFrameLayout, ReleaseOverviewFragment(), "releaseOverviewSearchResults")
//        transaction.commit()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                filter = query
            }
        }
    }
}
