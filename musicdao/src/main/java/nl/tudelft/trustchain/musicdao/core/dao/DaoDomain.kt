package nl.tudelft.trustchain.musicdao.core.dao

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

data class ProposalSignature(
    val proposalId: String,
    val bitcoinPublicKey: String,
    val trustchainPublicKey: String
)

sealed class Proposal {
    abstract val proposalCreator: String
    abstract val proposalId: String
    abstract val daoId: String
    abstract val proposalTime: String
    abstract val proposalTitle: String
    abstract val proposalText: String
    abstract val signaturesRequired: Int
    abstract val signatures: List<ProposalSignature>
    abstract val transferAmountBitcoinSatoshi: Int

    fun isClosed(): Boolean {
        return signatures.size >= signaturesRequired
    }
}

data class JoinProposal(
    override val proposalCreator: String,
    override val proposalId: String,
    override val daoId: String,
    override val proposalTime: String,
    override val proposalTitle: String,
    override val proposalText: String,
    override val signaturesRequired: Int,
    override val signatures: List<ProposalSignature>,
    override val transferAmountBitcoinSatoshi: Int
) : Proposal()

data class TransferProposal(
    override val signaturesRequired: Int,
    override val signatures: List<ProposalSignature>,
    override val transferAmountBitcoinSatoshi: Int,
    val transferAddress: String,
    override val proposalCreator: String,
    override val proposalId: String,
    override val daoId: String,
    override val proposalTime: String,
    override val proposalTitle: String,
    override val proposalText: String
) : Proposal()

data class Member(
    val bitcoinPublicKey: String,
    val trustchainPublicKey: String
)

data class DAO(
    val daoId: String,
    val name: String,
    val about: String,
    val proposals: Map<Proposal, TrustChainBlock?>,
    val members: List<Member>,
    val threshHold: Int,
    val entranceFee: Long,
    val previousTransaction: String,
    val balance: Long
)
