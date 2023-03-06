package nl.tudelft.trustchain.detoks

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

data class ProposalViewModel(val senderPK: String, val blockType: String, val block: TrustChainBlock) {

}
