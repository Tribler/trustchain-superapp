package nl.tudelft.trustchain.musicdao.core.ipv8

import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.releasePublish.ReleasePublishBlockSigner
import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.releasePublish.ReleasePublishBlockValidator
import javax.inject.Inject

class SetupMusicCommunity @Inject constructor(
    private val musicCommunity: MusicCommunity,
    private val releasePublishBlockSigner: ReleasePublishBlockSigner,
    private val releasePublishBlockValidator: ReleasePublishBlockValidator
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
