import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.EMPTY_PK
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.messaging.EndpointAggregator
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.util.TrustChainHelper
import nl.tudelft.trustchain.common.util.VotingHelper
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

private val lazySodium = LazySodiumJava(SodiumJava())


class VotingHelperTest {

    private fun getCommunity(): TrustChainCommunity {
        val settings = TrustChainSettings()
        val store = mockk<TrustChainStore>(relaxed = true)
        val community = TrustChainCommunity(settings = settings, database = store)
        community.myPeer = getMyPeer()
        community.endpoint = getEndpoint()
        community.network = Network()
        community.maxPeers = 20
        community.cryptoProvider = JavaCryptoProvider
        community.load()
        return community
    }

    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        testDispatcher.cleanupTestCoroutines()
    }

    protected fun getPrivateKey(): PrivateKey {
        val privateKey = "81df0af4c88f274d5228abb894a68906f9e04c902a09c68b9278bf2c7597eaf6"
        val signSeed = "c5c416509d7d262bddfcef421fc5135e0d2bdeb3cb36ae5d0b50321d766f19f2"
        return LibNaClSK(privateKey.hexToBytes(), signSeed.hexToBytes(), lazySodium)
    }

    protected fun getMyPeer(): Peer {
        return Peer(getPrivateKey())
    }

    protected fun getEndpoint(): EndpointAggregator {
        return spyk(EndpointAggregator(mockk(relaxed = true), null))
    }


    @Test
    fun startVote() {
        val community = getCommunity()
        val store = community.database
        every { store.getLatest(any(), any()) } returns null
        every { store.contains(any()) } returns false
        every { store.addBlock(any()) } returns Unit
        every { store.getBlockBefore(any()) } returns null
        every { store.getBlockAfter(any()) } returns null

        val helper = TrustChainHelper(community)
        val votingHelper = VotingHelper(community)

        // Add a validator to the community
        val validator = mockk<TransactionValidator>()
        every { validator.validate(any(), any()) } returns true
        community.registerTransactionValidator("voting_block", validator)

        // Create list of your peers and include yourself
        val peers: MutableList<PublicKey> = ArrayList()
        peers.addAll(community.getPeers().map { it.publicKey })
        peers.add(community.myPeer.publicKey)

        val voteSubject = "There should be tests"
        votingHelper.startVote(voteSubject, peers)

        // Verify that the proposal block has been casted
        assert(
            helper.getChainByUser(community.myPeer.publicKey.keyToBin()).any {
                JSONObject(it.transaction["message"].toString()).get("VOTE_SUBJECT") == voteSubject
            }
        )
    }

    @Test
    fun respondToVote() {
    }

    @Test
    fun countVotes() {
        val community = getCommunity()
        val store = community.database
        every { store.getLatest(any(), any()) } returns null
        every { store.contains(any()) } returns false
        every { store.addBlock(any()) } returns Unit
        every { store.getBlockBefore(any()) } returns null
        every { store.getBlockAfter(any()) } returns null

        val helper = TrustChainHelper(community)
        val votingHelper = VotingHelper(community)

        // Add a validator to the community
        val validator = mockk<TransactionValidator>()
        every { validator.validate(any(), any()) } returns true
        community.registerTransactionValidator("voting_block", validator)

        // Create list of your peers and include yourself
        val peers: MutableList<PublicKey> = ArrayList()
        peers.addAll(community.getPeers().map { it.publicKey })
        peers.add(community.myPeer.publicKey)

        val voteSubject = "There should be tests"

        // Launch proposition
        val voteList = JSONArray(peers)

        // Create a JSON object containing the vote subject, as well as a log of the eligible voters
        val voteJSONProp = JSONObject()
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", voteList)

        val transactionProp = voteJSONProp.toString()
        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to transactionProp),
            EMPTY_PK
        )


        // Create a reply agreement block
        val voteJSON = JSONObject()
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_REPLY", "YES")

        // Put the JSON string in the transaction's 'message' field.
        val transaction = mapOf("message" to voteJSON.toString())

        community.createAgreementBlock(propBlock, transaction)

        val count = votingHelper.countVotes(peers, voteSubject, propBlock.publicKey)
        Assert.assertEquals(Pair(0, 1), count)
    }
}
