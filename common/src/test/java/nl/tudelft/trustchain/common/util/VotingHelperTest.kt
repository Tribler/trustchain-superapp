package nl.tudelft.trustchain.common.util

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import io.mockk.mockk
import io.mockk.spyk
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.EMPTY_PK
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.keyvault.*
import nl.tudelft.ipv8.messaging.EndpointAggregator
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class VotingHelperTest {

    private val lazySodium = LazySodiumJava(SodiumJava())

    private fun createTrustChainStore(): TrustChainSQLiteStore {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        val database = Database(driver)
        return TrustChainSQLiteStore(database)
    }

    private fun getPeers(excludeSelf: Boolean = false): MutableList<PublicKey> {
        val peers: MutableList<PublicKey> = ArrayList()
        peers.add(defaultCryptoProvider.generateKey().pub())
        peers.add(defaultCryptoProvider.generateKey().pub())
        peers.add(defaultCryptoProvider.generateKey().pub())
        if (!excludeSelf) {
            peers.add(getMyPeer().publicKey)
        }
        return peers
    }

    private fun getCommunity(): TrustChainCommunity {
        val settings = TrustChainSettings()
        val store = createTrustChainStore()
        val community = TrustChainCommunity(settings = settings, database = store)
        community.myPeer = getMyPeer()
        community.endpoint = getEndpoint()
        community.network = Network()
        community.maxPeers = 20
        community.cryptoProvider = JavaCryptoProvider
        return community
    }

    private fun getPrivateKey(): PrivateKey {
        val privateKey = "81df0af4c88f274d5228abb894a68906f9e04c902a09c68b9278bf2c7597eaf6"
        val signSeed = "c5c416509d7d262bddfcef421fc5135e0d2bdeb3cb36ae5d0b50321d766f19f2"
        return LibNaClSK(privateKey.hexToBytes(), signSeed.hexToBytes(), lazySodium)
    }

    private fun getMyPeer(): Peer {
        return Peer(getPrivateKey())
    }

    private fun getEndpoint(): EndpointAggregator {
        return spyk(EndpointAggregator(mockk(relaxed = true), null))
    }

    @Test
    fun startVote() {
        val community = spyk(getCommunity())

        val helper = TrustChainHelper(community)
        val votingHelper = VotingHelper(community)
        val peers = getPeers()

        val voteSubject = "There should be tests"
        votingHelper.startVote(voteSubject, peers, VotingMode.YESNO)

        // Verify that the proposal block has been casted
        assert(
            helper.getChainByUser(community.myPeer.publicKey.keyToBin()).any {
                JSONObject(it.transaction["message"].toString()).get("VOTE_SUBJECT") == voteSubject
            }
        )
    }

    @Test
    fun respondToVote() {
        val community = spyk(getCommunity())
        val votingHelper = VotingHelper(community)
        val peers = getPeers()

        // Launch proposition
        val voteSubject = "A vote should be counted"
        val voteJSON = JSONObject()
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", peers)

        val transaction = voteJSON.toString()

        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to transaction),
            EMPTY_PK
        )

        // Create a reply agreement block
        votingHelper.respondToVote(true, propBlock)

        val helper = TrustChainHelper(community)
        assert(
            helper.getChainByUser(community.myPeer.publicKey.keyToBin()).any {
                JSONObject(it.transaction["message"].toString()).get("VOTE_REPLY") == "YES"
            }
        )
    }

    @Test
    fun countVotes() {
        val community = spyk(getCommunity())
        val votingHelper = VotingHelper(community)
        val peers = getPeers()

        // Launch proposition
        val voteSubject = "A vote should be counted"
        val voteJSON = JSONObject()
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", peers)

        val transaction = voteJSON.toString()

        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to transaction),
            EMPTY_PK
        )

        // Create a reply agreement block
        votingHelper.respondToVote(true, propBlock)

        val count =
            votingHelper.countVotes(peers, voteSubject, community.myPeer.publicKey.keyToBin())

        Assert.assertEquals(Pair(1, 0), count)
    }

    @Test
    fun preventDoubleVoteCount() {
        val community = spyk(getCommunity())
        val votingHelper = VotingHelper(community)
        val peers = getPeers()

        // Launch proposition
        val voteSubject = "A vote should be counted"
        val voteJSON = JSONObject()
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", peers)

        val transaction = voteJSON.toString()

        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to transaction),
            EMPTY_PK
        )

        // Create some reply agreement blocks
        votingHelper.respondToVote(true, propBlock)
        votingHelper.respondToVote(false, propBlock)
        votingHelper.respondToVote(true, propBlock)

        val count =
            votingHelper.countVotes(peers, voteSubject, community.myPeer.publicKey.keyToBin())

        Assert.assertEquals(Pair(1, 0), count)
    }

    @Test
    fun countEligibleVotes() {
        val community = spyk(getCommunity())
        val votingHelper = VotingHelper(community)
        val peers = getPeers(excludeSelf = true)

        // Launch proposition
        val voteSubject = "A vote should be counted"
        val voteJSON = JSONObject()
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", peers)

        val transaction = voteJSON.toString()

        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to transaction),
            EMPTY_PK
        )

        // Create a reply agreement block
        votingHelper.respondToVote(true, propBlock)

        val count =
            votingHelper.countVotes(peers, voteSubject, community.myPeer.publicKey.keyToBin())

        // verify my own vote has not been counted
        Assert.assertEquals(Pair(0, 0), count)
    }

    @Test
    fun checkIfCastedTrue() {
        val community = spyk(getCommunity())
        val votingHelper = VotingHelper(community)
        val peers = getPeers(excludeSelf = true)

        // Launch proposition
        val voteSubject = "A vote should be counted"
        val voteJSON = JSONObject()
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", peers)

        val transaction = voteJSON.toString()

        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to transaction),
            EMPTY_PK
        )

        // Create a reply agreement block
        votingHelper.respondToVote(true, propBlock)

        Assert.assertTrue(votingHelper.castedByPeer(propBlock, community.myPeer.publicKey) == Pair(1, 0))
    }

    @Test
    fun checkIfCastedFalse() {
        val community = spyk(getCommunity())
        val votingHelper = VotingHelper(community)
        val peers = getPeers(excludeSelf = true)

        // Launch proposition
        val voteSubject = "A vote should be counted"
        val voteJSON = JSONObject()
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", peers)

        val transaction = voteJSON.toString()

        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to transaction),
            EMPTY_PK
        )

        Assert.assertTrue(votingHelper.castedByPeer(propBlock, community.myPeer.publicKey) == Pair(0, 0))
    }

    @Test
    fun testVoteThresholdPass() {

        val community = spyk(getCommunity())
        val votingHelper = VotingHelper(community)
        val peers = getPeers()

        // Launch proposition
        val voteSubject = "There should be a threshold"
        val voteList = JSONArray(peers.map { i -> i.keyToBin().toHex()})
        val voteJSON = JSONObject()
            .put("VOTE_PROPOSER", community.myPeer.publicKey.keyToBin().toHex())
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", voteList)
            .put("VOTE_MODE", VotingMode.THRESHOLD)

        val transaction = voteJSON.toString()

        // Start the vote.
        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to transaction),
            EMPTY_PK
        )

        // Vote and thus make the threshold.
        votingHelper.respondToVote(true, propBlock)

        Assert.assertTrue(votingHelper.votingIsComplete(propBlock, 1))
    }

    @Test
    fun testVoteThresholdNoPass() {

        val community = spyk(getCommunity())
        val votingHelper = VotingHelper(community)
        val peers = getPeers()

        // Launch proposition
        val voteSubject = "There should be a threshold"
        val voteList = JSONArray(peers.map { i -> i.keyToBin().toHex()})
        val voteJSON = JSONObject()
            .put("VOTE_PROPOSER", community.myPeer.publicKey.keyToBin().toHex())
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", voteList)
            .put("VOTE_MODE", VotingMode.THRESHOLD)

        val transaction = voteJSON.toString()

        // Start the vote.
        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to transaction),
            EMPTY_PK
        )

        // Vote and thus make the threshold.
        votingHelper.respondToVote(false, propBlock)

        Assert.assertFalse(votingHelper.votingIsComplete(propBlock, 1))
    }

    @Test
    fun testVoteThresholdNoPassThresholdHigher() {

        val community = spyk(getCommunity())
        val votingHelper = VotingHelper(community)
        val peers = getPeers()

        // Launch proposition
        val voteSubject = "There should be a threshold"
        val voteList = JSONArray(peers.map { i -> i.keyToBin().toHex()})
        val voteJSON = JSONObject()
            .put("VOTE_PROPOSER", community.myPeer.publicKey.keyToBin().toHex())
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", voteList)
            .put("VOTE_MODE", VotingMode.THRESHOLD)

        val transaction = voteJSON.toString()

        // Start the vote.
        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to transaction),
            EMPTY_PK
        )

        // Vote and thus make the threshold.
        votingHelper.respondToVote(true, propBlock)

        Assert.assertFalse(votingHelper.votingIsComplete(propBlock, 5))
    }

    @Test
    fun testVoteCompleteWithoutThreshold() {

        val community = spyk(getCommunity())
        val votingHelper = VotingHelper(community)

        // Launch proposition
        val voteSubject = "There should be a threshold"
        val voteList = JSONArray(listOf(community.myPeer.publicKey.keyToBin().toHex()))
        val voteJSON = JSONObject()
            .put("VOTE_PROPOSER", community.myPeer.publicKey.keyToBin().toHex())
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", voteList)
            .put("VOTE_MODE", VotingMode.YESNO)

        val transaction = voteJSON.toString()

        // Start the vote.
        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to transaction),
            EMPTY_PK
        )

        // Vote and thus make the threshold.
        votingHelper.respondToVote(false, propBlock)

        Assert.assertTrue(votingHelper.votingIsComplete(propBlock))
    }

    @Test
    fun testVoteNotCompleteWithoutThreshold() {

        val community = spyk(getCommunity())
        val votingHelper = VotingHelper(community)
        val peers = getPeers()

        // Launch proposition
        val voteSubject = "There should be a threshold"
        val voteList = JSONArray(peers.map { i -> i.keyToBin().toHex()})
        val voteJSON = JSONObject()
            .put("VOTE_PROPOSER", community.myPeer.publicKey.keyToBin().toHex())
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", voteList)
            .put("VOTE_MODE", VotingMode.YESNO)

        val transaction = voteJSON.toString()

        // Start the vote.
        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to transaction),
            EMPTY_PK
        )

        // Vote and thus make the threshold.
        votingHelper.respondToVote(true, propBlock)

        Assert.assertFalse(votingHelper.votingIsComplete(propBlock))
    }



}
