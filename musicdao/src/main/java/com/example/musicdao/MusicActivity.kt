package com.example.musicdao

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.lifecycleScope
import com.example.musicdao.ipv8.MusicCommunity
import com.example.musicdao.services.MusicGossipingService
import com.example.musicdao.ui.MusicDAOApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

/**
 * This maintains the interactions between the UI and seeding/trustchain
 */
class MusicActivity : AppCompatActivity() {

    lateinit var container: AppContainer
    lateinit var mService: MusicGossipingService
    var mBound: Boolean = false

    @ExperimentalMaterialApi
    @ExperimentalComposeUiApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val musicCommunity = IPv8Android.getInstance().getOverlay<MusicCommunity>()
            ?: throw IllegalStateException("MusicCommunity is not configured")
        AppContainer.provide(this, musicCommunity = musicCommunity, this)
        container = AppContainer
        registerBlockSigner()
        container.releaseRepository.refreshReleases()
        iterativelyFetchReleases()
        iterativelyUpdateSwarmHealth()
        Intent(this, MusicGossipingService::class.java).also { intent ->
            startService(intent)
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            MusicDAOApp(container)
        }
    }

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

    /**
     * Show libtorrent connectivity stats
     */
    fun getStatsOverview(): String {
        return "Starting torrent client..."
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




    /**
     * On discovering a half block, with tag publish_release, agree it immediately (for now). In the
     * future there will be logic added here to determine whether an upload was done by the correct
     * artist/label (artist passport).
     */
    private fun registerBlockSigner() {
        val musicCommunity = IPv8Android.getInstance().getOverlay<MusicCommunity>()
        musicCommunity?.registerBlockSigner(
            "publish_release",
            object : BlockSigner {
                override fun onSignatureRequest(block: TrustChainBlock) {
                    Log.d("rian", "------ KOMT BLOK BINNEN")
                    musicCommunity.createAgreementBlock(block, mapOf<Any?, Any?>())
                }
            }
        )
    }

    /**
     * Keep track of Swarm Health for all torrents being monitored
     */
    private fun iterativelyUpdateSwarmHealth() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                container.swarmHealthRepository.mergedSwarmHealth =
                    container.swarmHealthRepository.filterSwarmHealthMap()

                if (mBound) {
                    mService.setSwarmHealthMap(container.swarmHealthRepository.mergedSwarmHealth)
                }
                delay(3000)
            }
        }
    }

    private fun iterativelyFetchReleases() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                container.releaseRepository.refreshReleases()
                delay(3000)
            }
        }
    }

    override fun startActivityForResult(intent: Intent?, requestCode: Int) {
        require(!(requestCode != -1 && requestCode and -0x10000 != 0)) { "Can only use lower 16 bits for requestCode" }
        super.startActivityForResult(intent, requestCode)
    }


}
