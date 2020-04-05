import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
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
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.keyvault.*
import nl.tudelft.ipv8.messaging.EndpointAggregator
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.util.TrustChainHelper
import nl.tudelft.trustchain.common.util.VotingHelper
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

private val lazySodium = LazySodiumJava(SodiumJava())


class VotingHelperTest {

    private fun createTrustChainStore(): TrustChainSQLiteStore {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        val database = Database(driver)
        return TrustChainSQLiteStore(database)
    }

    private fun getPeers(): List<PublicKey> {
        val peers: MutableList<PublicKey> = ArrayList()
        peers.add(defaultCryptoProvider.generateKey().pub())
        peers.add(defaultCryptoProvider.generateKey().pub())
        peers.add(defaultCryptoProvider.generateKey().pub())
        peers.add(getMyPeer().publicKey)

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
        val community = spyk(getCommunity())

        val helper = TrustChainHelper(community)
        val votingHelper = VotingHelper(community)

        // Create list of your peers and include yourself
        val peers: MutableList<PublicKey> = ArrayList()
        peers.add(defaultCryptoProvider.generateKey().pub())
        peers.add(defaultCryptoProvider.generateKey().pub())
        peers.add(defaultCryptoProvider.generateKey().pub())
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
        val community = spyk(getCommunity())
        val votingHelper = VotingHelper(community)

        // Create list of your peers and include yourself
        val peers: MutableList<PublicKey> = ArrayList()
        peers.add(defaultCryptoProvider.generateKey().pub())
        peers.add(defaultCryptoProvider.generateKey().pub())
        peers.add(defaultCryptoProvider.generateKey().pub())
        peers.add(community.myPeer.publicKey)

        // Launch proposition
        val voteSubject = "A vote should be counted"
        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to voteSubject),
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
        val propBlock = community.createProposalBlock(
            "voting_block",
            mapOf("message" to voteSubject),
            EMPTY_PK
        )

        // Create a reply agreement block
        votingHelper.respondToVote(true, propBlock)

        val helper = TrustChainHelper(community)
        helper.getChainByUser(community.myPeer.publicKey.keyToBin()).forEach {
            println(JSONObject(it.transaction["message"].toString()))
        }

        val count =
            votingHelper.countVotes(peers, voteSubject, community.myPeer.publicKey.keyToBin())

        Assert.assertEquals(Pair(0, 1), count)
    }


}
