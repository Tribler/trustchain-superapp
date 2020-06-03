package com.example.musicdao

//import kotlinx.android.synthetic.main.music_app_main.*
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.NavUtils
import androidx.preference.PreferenceManager
import com.frostwire.jlibtorrent.FileStorage
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

const val PREPARE_SIZE_KB: Long = 10 * 512L

class MusicService : BaseActivity() {
    private var currentMagnetLoading: String? = null
    public val trackLibrary: TrackLibrary =
        TrackLibrary()

    override val navigationGraph = R.navigation.musicdao_navgraph

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearCache()
        supportActionBar?.title = "Music app"

        registerBlockListener()
        registerBlockSigner()

//        val audioPlayer = AudioPlayer.getInstance(this)
//        if (audioPlayer.parent == null) {
//            mainLinearLayout.addView(AudioPlayer.getInstance(this), 0)
//        }

        Thread(Runnable {
            trackLibrary.startDHT()
        }).start()
    }

    fun prepareNextTrack() {
//        val trackPlayerFragment: AudioPlayer? = supportFragmentManager.findFragmentById(R.id.track_player_fragment)
//        trackPlayerFragment?.prepareNextTrack()
        val audioPlayer = supportFragmentManager.findFragmentById(R.id.track_player_fragment) as AudioPlayer
        audioPlayer.prepareNextTrack()
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

    /**
     * Once blocks on the trustchain arrive, which are audio release blocks, try to fetch and render
     * its metadata from its torrent file structure.
     */
    private fun registerBlockListener() {
        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
        val musicService = this
        trustchain.addListener("publish_release", object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Toast.makeText(
                    applicationContext,
                    "Discovered signed block ${block.blockId}",
                    Toast.LENGTH_LONG
                ).show()
                val magnet = block.transaction["magnet"]
                if (magnet != null && magnet is String) {
                    val release = Release(
                        magnet,
                        trackLibrary,
                        musicService,
                        block.transaction
                    )
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.add(R.id.trackListLinearLayout, release, "releaseTag")
                    transaction.commit()
                }
                Log.d("TrustChainDemo", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
    }

    companion object {
        private const val PREF_PRIVATE_KEY = "private_key"
    }

    private fun getPrivateKey(): PrivateKey {
        // Load a key from the shared preferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val privateKey = prefs.getString(PREF_PRIVATE_KEY, null)
        return if (privateKey == null) {
            // Generate a new key on the first launch
            val newKey = AndroidCryptoProvider.generateKey()
            prefs.edit()
                .putString(PREF_PRIVATE_KEY, newKey.keyToBin().toHex())
                .apply()
            newKey
        } else {
            AndroidCryptoProvider.keyFromPrivateBin(privateKey.hexToBytes())
        }
    }

    fun setSongArtistText(s: String) {
        val audioPlayer = supportFragmentManager.findFragmentById(R.id.track_player_fragment) as AudioPlayer
        audioPlayer.setSongArtistText(s)
    }

    fun startPlaying(file: File, index: Int, files: FileStorage) {
        val audioPlayer = supportFragmentManager.findFragmentById(R.id.track_player_fragment) as AudioPlayer
        audioPlayer.setAudioResource(file, index, files)
    }
}
