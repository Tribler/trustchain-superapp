package com.example.musicdao.core.ipv8

import android.util.Log
import com.example.musicdao.core.ipv8.signers.ReleasePublishBlockSigner
import com.example.musicdao.core.ipv8.signers.ReleasePublishBlockValidator
import javax.inject.Inject

class SetupMusicCommunity @Inject constructor(
    private val musicCommunity: MusicCommunity,
    private val releasePublishBlockSigner: ReleasePublishBlockSigner,
    private val releasePublishBlockValidator: ReleasePublishBlockValidator,
) {

    fun registerListeners() {
        musicCommunity.registerTransactionValidator(
            ReleasePublishBlockValidator.BLOCK_TYPE,
            releasePublishBlockValidator
        )
        musicCommunity.registerBlockSigner(
            ReleasePublishBlockSigner.BLOCK_TYPE,
            releasePublishBlockSigner
        )
    }
}
