package nl.tudelft.trustchain.literaturedao.ipv8

import nl.tudelft.ipv8.Peer

data class EvaDownload(
    var activeDownload: Boolean = false,
    var lastRequest: Long? = null,
    var magnetInfoHash: String = "",
    var peer: Peer? = null,
    var retryAttempts: Int = 0,
    var fileName: String = ""
)
