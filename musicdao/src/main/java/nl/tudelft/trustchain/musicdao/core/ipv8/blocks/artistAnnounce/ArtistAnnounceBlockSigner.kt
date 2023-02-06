package nl.tudelft.trustchain.musicdao.core.ipv8.blocks.artistAnnounce

import nl.tudelft.trustchain.musicdao.core.ipv8.MusicCommunity
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import javax.inject.Inject

class ArtistAnnounceBlockSigner @Inject constructor(val musicCommunity: MusicCommunity) :
    BlockSigner {
    override fun onSignatureRequest(block: TrustChainBlock) {
        musicCommunity.createAgreementBlock(block, mapOf<Any?, Any?>())
    }
}
