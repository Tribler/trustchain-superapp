package com.example.musicdao

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.example.musicdao.ui.SubmitReleaseDialog
import com.frostwire.jlibtorrent.FileStorage
import kotlinx.android.synthetic.main.fragment_release_overview.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.BaseActivity
import java.io.File

class MusicService : BaseActivity() {
    val trackLibrary: TrackLibrary =
        TrackLibrary()
    override val navigationGraph = R.navigation.musicdao_navgraph

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearCache()

        registerBlockSigner()

        Thread(Runnable {
            trackLibrary.startDHT()
        }).start()
    }

    /**
     * Clear cache on every run (for testing, and audio files may be large 15MB+). May be removed
     * in the future
     */
    private fun clearCache() {
        if (cacheDir.isDirectory && cacheDir.listFiles() != null) {
            val files = cacheDir.listFiles()
            files?.forEach {
                it.deleteRecursively()
            }
        }
    }

    private fun registerBlockSigner() {
        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
        trustchain.registerBlockSigner("publish_release", object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                Toast.makeText(
                    applicationContext,
                    "Signing block ${block.blockId}",
                    Toast.LENGTH_LONG
                ).show()
                trustchain.createAgreementBlock(block, mapOf<Any?, Any?>())
            }
        })
    }
}
