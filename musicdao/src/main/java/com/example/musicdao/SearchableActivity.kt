package com.example.musicdao

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle

class SearchableActivity : MusicService() {
    override val navigationGraph = R.navigation.musicdao_navgraph

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
