package com.example.musicdao

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.lifecycleScope
import com.example.musicdao.core.ipv8.SetupMusicCommunity
import com.example.musicdao.core.repositories.AlbumRepository
import com.example.musicdao.core.repositories.MusicGossipingService
import com.example.musicdao.core.torrent.TorrentCache
import com.example.musicdao.core.wallet.WalletService
import com.example.musicdao.ui.MusicDAOApp
import com.example.musicdao.ui.screens.profile.ProfileScreenViewModel
import com.example.musicdao.ui.screens.release.ReleaseScreenViewModel
import com.frostwire.jlibtorrent.SessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * This maintains the interactions between the UI and seeding/trustchain
 */
@AndroidEntryPoint
class MusicActivity : AppCompatActivity() {

    lateinit var mService: MusicGossipingService
    var mBound: Boolean = false

    @Inject
    lateinit var albumRepository: AlbumRepository

    @Inject
    lateinit var torrentCache: TorrentCache

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var walletService: WalletService

    @Inject
    lateinit var setupMusicCommunity: SetupMusicCommunity

    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @RequiresApi(Build.VERSION_CODES.O)
    @ExperimentalMaterialApi
    @ExperimentalComposeUiApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.provide(this)
        lifecycleScope.launchWhenStarted {
            setupMusicCommunity.registerListeners()
            albumRepository.refreshCache()
            torrentCache.seedStrategy()
            GlobalScope.launch(Dispatchers.IO) {
                walletService.start()
            }
        }
        iterativelyFetchReleases()
        Intent(this, MusicGossipingService::class.java).also { intent ->
            startService(intent)
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            MusicDAOApp()
        }
    }

    /**
     * On discovering a half block, with tag publish_release, agree it immediately (for now). In the
     * future there will be logic added here to determine whether an upload was done by the correct
     * artist/label (artist passport).
     */
    override fun onDestroy() {
        super.onDestroy()
        if (mBound) {
            unbindService(mConnection)
        }
    }

    private val mConnection = object : ServiceConnection {
        // Called when the connection with the service is established
        override fun onServiceConnected(className: ComponentName, service: IBinder) {

            val binder = service as MusicGossipingService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        // Called when the connection with the service disconnects unexpectedly
        override fun onServiceDisconnected(className: ComponentName) {
            mBound = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uris = uriListFromLocalFiles(intent = data!!)
        AppContainer.currentCallback(uris)
    }

    fun uriListFromLocalFiles(intent: Intent): List<Uri> {
        // This should be reached when the chooseFile intent is completed and the user selected
        // an audio file
        val uriList = mutableListOf<Uri>()
        val singleFileUri = intent.data
        if (singleFileUri != null) {
            // Only one file is selected
            uriList.add(singleFileUri)
        }
        val clipData = intent.clipData
        if (clipData != null) {
            // Multiple files are selected
            val count = clipData.itemCount
            for (i in 0 until count) {
                val uri = clipData.getItemAt(i).uri
                uriList.add(uri)
            }
        }
        return uriList
    }

    private fun iterativelyFetchReleases() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                albumRepository.refreshCache()
                delay(3000)
            }
        }
    }

    override fun startActivityForResult(intent: Intent?, requestCode: Int) {
        require(!(requestCode != -1 && requestCode and -0x10000 != 0)) { "Can only use lower 16 bits for requestCode" }
        super.startActivityForResult(intent, requestCode)
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface ViewModelFactoryProvider {
        fun noteDetailViewModelFactory(): ReleaseScreenViewModel.ReleaseScreenViewModelFactory
        fun profileScreenViewModelFactory(): ProfileScreenViewModel.ProfileScreenViewModelFactory
    }
}
