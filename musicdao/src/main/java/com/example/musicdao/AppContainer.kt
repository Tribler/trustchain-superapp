package com.example.musicdao

import android.content.Context
import com.example.musicdao.net.ContentSeeder
import com.frostwire.jlibtorrent.SessionManager

/**
 * Simple manual dependency injection. Is destroyed
 * when the main activity is destroyed.
 */
interface AppContainer {
    val contentSeeder: ContentSeeder
}

class AppContainerImpl(
    applicationContext: Context,
    sessionManager: SessionManager
) : AppContainer {
    override val contentSeeder = ContentSeeder(applicationContext.cacheDir, sessionManager)
}
