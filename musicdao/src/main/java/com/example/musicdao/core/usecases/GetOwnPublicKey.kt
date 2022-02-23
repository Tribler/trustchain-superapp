package com.example.musicdao.core.usecases

import com.example.musicdao.core.ipv8.MusicCommunity
import nl.tudelft.ipv8.util.toHex
import javax.inject.Inject

class GetOwnPublicKey @Inject constructor(
    private val musicCommunity: MusicCommunity,
) {

    operator fun invoke(): String {
        return musicCommunity.myPeer.publicKey.keyToBin().toHex()
    }
}
