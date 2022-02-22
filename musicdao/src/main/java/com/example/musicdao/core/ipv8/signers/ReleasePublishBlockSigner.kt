package com.example.musicdao.core.ipv8.signers

import android.util.Log
import com.example.musicdao.core.ipv8.MusicCommunity
import com.example.musicdao.core.ipv8.blocks.ReleasePublishBlock
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import javax.inject.Inject

class ReleasePublishBlockSigner @Inject constructor(val musicCommunity: MusicCommunity) :
    BlockSigner {
    override fun onSignatureRequest(block: TrustChainBlock) {
        musicCommunity.createAgreementBlock(block, mapOf<Any?, Any?>())
    }

    companion object {
        val BLOCK_TYPE = ReleasePublishBlock.BLOCK_TYPE
    }
}

