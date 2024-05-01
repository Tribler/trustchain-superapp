package nl.tudelft.trustchain.currencyii.leaderElection

import android.content.Context
import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.keyvault.Key
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.messaging.EndpointAggregator
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.payload.ElectionPayload
import nl.tudelft.trustchain.currencyii.payload.SignPayload
import nl.tudelft.trustchain.currencyii.sharedWallet.SWResponseSignatureBlockTD
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskBlockTD
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Calendar

class PayloadTest {


    @Test
    fun alivePayloadTest() {
        val context = mockk<Context>()
        val coinCommunity = CoinCommunity(context)
        val dAOid = "Dao_id"
        val me = mockk<Peer>()

        val meKey = mockk<PublicKey>()

        coinCommunity.myPeer = me

        every { meKey.keyToBin() } returns "me_key".toByteArray()

        every { me.publicKey } returns meKey
        every { me.lamportTimestamp } returns 0u

        every { me.updateClock(any<ULong>()) } returns Unit
        every { me.key } returns mockk<Key>()

        val daoIdBytes = dAOid.toByteArray()
        val packet = coinCommunity.createAliveResponse(daoIdBytes)
        val packetLastElements = packet.takeLast(daoIdBytes.size)

        assertEquals(daoIdBytes.toList(), packetLastElements)
    }

    @Test
    fun electionPayloadTest() {
        val context = mockk<Context>()
        val coinCommunity = CoinCommunity(context)
        val dAOid = "Dao_id"
        val me = mockk<Peer>()

        val meKey = mockk<PublicKey>()

        coinCommunity.myPeer = me

        every { meKey.keyToBin() } returns "me_key".toByteArray()

        every { me.publicKey } returns meKey
        every { me.lamportTimestamp } returns 0u

        every { me.updateClock(any<ULong>()) } returns Unit
        every { me.key } returns mockk<Key>()

        val daoIdBytes = dAOid.toByteArray()
        val packet = coinCommunity.createElectedResponse(daoIdBytes)
        val packetLastElements = packet.takeLast(daoIdBytes.size)

        assertEquals(daoIdBytes.toList(), packetLastElements)
    }

    @Test
    fun electionPayloadSerializeTest() {

        val context = mockk<Context>()
        val coinCommunity = CoinCommunity(context)
        val dAOid = "Dao_id"
        val me = mockk<Peer>()
        val meKey = mockk<PublicKey>()

        coinCommunity.myPeer = me
        every { meKey.keyToBin() } returns "me_key".toByteArray()

        every { me.publicKey } returns meKey
        every { me.lamportTimestamp } returns 0u

        every { me.updateClock(any<ULong>()) } returns Unit
        every { me.key } returns mockk<Key>()

        val sig1 = SWResponseSignatureBlockTD("SignatureId1", "ProposalId", "SignatureId1Serialized", "BTC_PK1", "NONCE1")
        val sig2 = SWResponseSignatureBlockTD("SignatureId2", "ProposalId", "SignatureId2Serialized", "BTC_PK2", "NONCE2")
        val sig3 = SWResponseSignatureBlockTD("SignatureId3", "ProposalId", "SignatureId3Serialized", "BTC_PK3", "NONCE3")

        val daoIdBytes = dAOid.toByteArray()
        val signatures = listOf(sig1, sig2, sig3)
        val trustChainBlock = TrustChainBlock("type", "rawTransaction".toByteArray(), "publicKey".toByteArray(), 42U, "linkPublicKey".toByteArray(),
            12U, "previousHash".toByteArray(), "signature".toByteArray(), Calendar.getInstance().time)
        val proposeBlockData = SWSignatureAskBlockTD("SW_UNIQUE_ID", "SW_UNIQUE_PROPOSAL_ID",
            "SW_TRANSACTION_SERIALIZED", "SW_PREVIOUS_BLOCK_HASH", 5, "SW_RECEIVER_PK")

        val payload = SignPayload(daoIdBytes, trustChainBlock, proposeBlockData, signatures)
        val serialized = payload.serialize()
        val deserialized = SignPayload.deserialize(serialized)


        assertEquals(payload.DAOid.decodeToString(), deserialized.first.DAOid.decodeToString())
        assertEquals(payload.mostRecentSWBlock.type, deserialized.first.mostRecentSWBlock.type)
        assertEquals(payload.mostRecentSWBlock.rawTransaction.decodeToString(), deserialized.first.mostRecentSWBlock.rawTransaction.decodeToString())
        assertEquals(payload.mostRecentSWBlock.publicKey.decodeToString(), deserialized.first.mostRecentSWBlock.publicKey.decodeToString())
        assertEquals(payload.mostRecentSWBlock.sequenceNumber, deserialized.first.mostRecentSWBlock.sequenceNumber)
        assertEquals(payload.mostRecentSWBlock.linkPublicKey.decodeToString(), deserialized.first.mostRecentSWBlock.linkPublicKey.decodeToString())
        assertEquals(payload.mostRecentSWBlock.linkSequenceNumber, deserialized.first.mostRecentSWBlock.linkSequenceNumber)
        assertEquals(payload.mostRecentSWBlock.previousHash.decodeToString(), deserialized.first.mostRecentSWBlock.previousHash.decodeToString())
        assertEquals(payload.mostRecentSWBlock.signature.decodeToString(), deserialized.first.mostRecentSWBlock.signature.decodeToString())
        assertEquals(payload.mostRecentSWBlock.timestamp.time, deserialized.first.mostRecentSWBlock.timestamp.time)

        assertEquals(payload.signatures.first().SW_UNIQUE_ID, deserialized.first.signatures.first().SW_UNIQUE_ID)
        assertEquals(payload.signatures.first().SW_BITCOIN_PK, deserialized.first.signatures.first().SW_BITCOIN_PK)
        assertEquals(payload.signatures.last().SW_NONCE, deserialized.first.signatures.last().SW_NONCE)
        assertEquals(payload.signatures.size, deserialized.first.signatures.size)
        assertEquals(payload.signatures.last().SW_UNIQUE_PROPOSAL_ID, deserialized.first.signatures.last().SW_UNIQUE_PROPOSAL_ID)

        assertEquals(payload.proposeBlockData.SW_UNIQUE_PROPOSAL_ID, deserialized.first.proposeBlockData.SW_UNIQUE_PROPOSAL_ID)
        assertEquals(payload.proposeBlockData.SW_TRANSACTION_SERIALIZED, deserialized.first.proposeBlockData.SW_TRANSACTION_SERIALIZED)
        assertEquals(payload.proposeBlockData.SW_SIGNATURES_REQUIRED, deserialized.first.proposeBlockData.SW_SIGNATURES_REQUIRED)
        assertEquals(payload.proposeBlockData.SW_PREVIOUS_BLOCK_HASH, deserialized.first.proposeBlockData.SW_PREVIOUS_BLOCK_HASH)
        assertEquals(payload.proposeBlockData.SW_UNIQUE_ID, deserialized.first.proposeBlockData.SW_UNIQUE_ID)
        assertEquals(payload.proposeBlockData.SW_RECEIVER_PK, deserialized.first.proposeBlockData.SW_RECEIVER_PK)


    }
}

class LeaderElectionTest {
    private lateinit var lazySodium: LazySodiumJava
    private lateinit var key: LibNaClSK

    private lateinit var myPeer: Peer

    private lateinit var community: CoinCommunity
    private lateinit var network: Network
    private lateinit var endpoint: EndpointAggregator
    private lateinit var handler: (Packet) -> Unit

    fun init() {
        val lazySodium = LazySodiumJava(SodiumJava())
        val key =
            LibNaClSK(
                "81df0af4c88f274d5228abb894a68906f9e04c902a09c68b9278bf2c7597eaf6".hexToBytes(),
                "c5c416509d7d262bddfcef421fc5135e0d2bdeb3cb36ae5d0b50321d766f19f2".hexToBytes(),
                lazySodium
            )

        val myPeer = Peer(key)

        val context = mockk<Context>()
        val coinCommunity = CoinCommunity(context)
        val network = Network()
        val endpoint = spyk(EndpointAggregator(mockk(relaxed = true), null))
        val handler = mockk<(Packet) -> Unit>(relaxed = true)

        this.lazySodium = lazySodium
        this.key = key
        this.myPeer = myPeer
        this.network = network
        this.endpoint = endpoint
        this.handler = handler
        this.community = community

        community.myPeer = myPeer
        community.endpoint = endpoint
        community.network = network
        community.evaProtocolEnabled = true
    }

    @Test
    fun onAlivePacketTest() {
        init()
        community.messageHandlers[CoinCommunity.MessageId.ALIVE_RESPONSE] = handler
        community.createAliveResponse(
            "x".repeat(64).toByteArray()
        ).let { packet ->
            community.onPacket(Packet(myPeer.address, packet))
        }
        verify { handler(any()) }
    }

    @Test
    fun handleAlivePacketTest() {
        init()
        val spykedCommunity = spyk(community)
        spykedCommunity.createAliveResponse(
            "x".repeat(64).toByteArray()
        ).let { packet ->
            println(packet.size)
            spykedCommunity.onAliveResponsePacket(Packet(myPeer.address, packet))
        }
        verify { spykedCommunity.onAliveResponse(any(), any()) }
        verify { spykedCommunity.getCandidates() }
    }

    @Test
    fun onElectedPacketTest() {
        init()
        community.messageHandlers[CoinCommunity.MessageId.ELECTED_RESPONSE] = handler
        community.createElectedResponse(
            "x".repeat(64).toByteArray()
        ).let { packet ->
            community.onPacket(Packet(myPeer.address, packet))
        }
        verify { handler(any()) }
    }

    @Test
    fun handleElectedPacketTest() {
        init()
        val spykedCommunity = spyk(community)
        spykedCommunity.createElectedResponse(
            "x".repeat(64).toByteArray()
        ).let { packet ->
            println(packet.size)
            spykedCommunity.onElectedResponsePacket(Packet(myPeer.address, packet))
        }
        verify { spykedCommunity.onElectedResponse(any(), any()) }
    }

    @Test
    fun onElectionPacketTest() {
        init()
        community.messageHandlers[CoinCommunity.MessageId.ELECTION_REQUEST] = handler
        community.createElectionRequest(
            "x".repeat(64).toByteArray()
        ).let { packet ->
            community.onPacket(Packet(myPeer.address, packet))
        }
        verify { handler(any()) }
    }
}

class OnElectionpayloadTest() {
    companion object {
        lateinit var community: CoinCommunity
        lateinit var candidates: HashMap<String, ArrayList<Peer>>
        lateinit var currentLeader: HashMap<String, Peer?>

        lateinit var ipv4P1: IPv4Address
        lateinit var ipv4P2: IPv4Address
        lateinit var ipv4P3: IPv4Address
        lateinit var ipv4P4: IPv4Address

        lateinit var key1: PublicKey
        lateinit var key2: PublicKey
        lateinit var key3: PublicKey
        lateinit var key4: PublicKey

        lateinit var peer1: Peer
        lateinit var peer2: Peer
        lateinit var peer3: Peer
        lateinit var peer4: Peer
        val daoID: ByteArray = "x".repeat(64).toByteArray()

        @BeforeAll
        @JvmStatic
        fun setup() {
            community = mockk<CoinCommunity>(relaxed = true)
            candidates = HashMap()
            currentLeader = HashMap()
            every { community.getCandidates() } returns candidates
            every { community.getCurrentLeader() } returns currentLeader

            ipv4P1 = mockk<IPv4Address>()
            ipv4P2 = mockk<IPv4Address>()
            ipv4P3 = mockk<IPv4Address>()
            ipv4P4 = mockk<IPv4Address>()

            peer1 = mockk<Peer>()
            peer2 = mockk<Peer>()
            peer3 = mockk<Peer>()
            peer4 = mockk<Peer>()

            key1 = mockk<PublicKey>()
            key2 = mockk<PublicKey>()
            key3 = mockk<PublicKey>()
            key4 = mockk<PublicKey>()

            every { ipv4P1.hashCode() } returns 1
            every { ipv4P2.hashCode() } returns 2
            every { ipv4P3.hashCode() } returns 3
            every { ipv4P4.hashCode() } returns 4

            every { peer1.address } returns ipv4P1
            every { peer2.address } returns ipv4P2
            every { peer3.address } returns ipv4P3
            every { peer4.address } returns ipv4P4

            every { peer1.publicKey } returns key1
            every { peer2.publicKey } returns key2
            every { peer3.publicKey } returns key3
            every { peer4.publicKey } returns key4

            every { key1.keyToBin() } returns "1".toByteArray()
            every { key2.keyToBin() } returns "2".toByteArray()
            every { key3.keyToBin() } returns "3".toByteArray()
            every { key4.keyToBin() } returns "4".toByteArray()

            val retPK: ArrayList<String> = ArrayList()
            retPK.add("1")
            retPK.add("2")
            retPK.add("3")
            retPK.add("4")

            every { community.getPeersPKInDao(any()) } returns retPK

            every {
                community.onElectionRequest(
                    any(),
                    any()
                )
            } answers { callOriginal() }

            every { community.createAliveResponse(any()) } answers { callOriginal() }
            every { community.createElectedResponse(any()) } answers { callOriginal() }
            every { community.sendPayload(any(), any()) } just runs
        }
    }

    @Test
    fun onElectionRequestTestLargest() {
        every { community.myPeer } returns peer4
        every { community.getPeers() } answers { listOf(peer1, peer2, peer3) }
        val alivePacket = community.createAliveResponse(daoID)
        val electedPacket = community.createElectedResponse(daoID)

        community.onElectionRequest(peer3, ElectionPayload(daoID))

        verify { community.sendPayload(peer3, alivePacket) }
        verify { community.sendPayload(peer3, electedPacket) }
        verify { community.getPeers() }

        verify { ipv4P1.hashCode() }
        verify { ipv4P2.hashCode() }
        verify { ipv4P3.hashCode() }

        verify { key1.keyToBin() }
        verify { key2.keyToBin() }
        verify { key3.keyToBin() }

        community.getCurrentLeader()[daoID.decodeToString()]?.let { assert(it.equals(peer4)) }
    }

    @Test
    fun onElectionRequestNotLargestNoReplies() {
        every { community.myPeer } returns peer2
        every { community.getPeers() } answers { listOf(peer1, peer3, peer4) }

        val alivePacket = community.createAliveResponse(daoID)
        val electionPacket = community.createElectionRequest(daoID)
        val electedPacket = community.createElectedResponse(daoID)

        community.onElectionRequest(peer1, ElectionPayload(daoID))

        verify { community.sendPayload(peer1, alivePacket) }
        verify { community.sendPayload(peer1, electedPacket) }

        verify { community.sendPayload(peer3, electionPacket) }
        verify { community.sendPayload(peer4, electionPacket) }

        verify { community.getPeers() }

        verify { ipv4P1.hashCode() }
        verify { ipv4P3.hashCode() }
        verify { ipv4P4.hashCode() }

        verify { key1.keyToBin() }
        verify { key3.keyToBin() }
        verify { key4.keyToBin() }

        community.getCurrentLeader()[daoID.decodeToString()]?.let { assert(it.equals(peer2)) }
    }

    @Test
    fun onElectionRequestNotLargestOneReply() {
        every { community.myPeer } returns peer2
        every { community.getPeers() } answers { listOf(peer1, peer3, peer4) }
        candidates[daoID.decodeToString()] = ArrayList()
        candidates[daoID.decodeToString()]?.add(peer3)
        val alivePacket = community.createAliveResponse(daoID)
        val electionPacket = community.createElectionRequest(daoID)
        val electedPacket = community.createElectedResponse(daoID)

        community.onElectionRequest(peer1, ElectionPayload(daoID))

        verify { community.sendPayload(peer1, alivePacket) }
        verify { community.sendPayload(peer1, electedPacket) }

        verify { community.sendPayload(peer3, electionPacket) }
        verify { community.sendPayload(peer4, electionPacket) }

        verify { community.getPeers() }

        verify { ipv4P1.hashCode() }
        verify { ipv4P3.hashCode() }
        verify { ipv4P4.hashCode() }

        verify { key1.keyToBin() }
        verify { key3.keyToBin() }
        verify { key4.keyToBin() }

        assert(community.getCurrentLeader()[daoID.decodeToString()] == null)
    }
}

class LeaderSignProposalTests() {
    companion object {
        lateinit var currentLeader: HashMap<String, Peer?>
        lateinit var community: CoinCommunity

        lateinit var mostRecentSWBlock: TrustChainBlock
        lateinit var proposeBlockData: SWSignatureAskBlockTD
        lateinit var signatures: List<SWResponseSignatureBlockTD>
        lateinit var context: Context

        lateinit var peer1: Peer
        lateinit var peer2: Peer
        lateinit var peer3: Peer
        lateinit var peer4: Peer

        lateinit var ipv4P1: IPv4Address
        lateinit var ipv4P2: IPv4Address
        lateinit var ipv4P3: IPv4Address
        lateinit var ipv4P4: IPv4Address

        val daoID: ByteArray = "x".repeat(64).toByteArray()

        val mostRecentSWBlockArray: String = "y".repeat(64)
        val proposeBlockDataArray: String = "p".repeat(64)
        val contextArray: String = "c".repeat(64)

        val electionPacket: ByteArray = "e".repeat(64).toByteArray()

        @BeforeAll
        @JvmStatic
        fun setup() {
            community = mockk<CoinCommunity>(relaxed = true)

            peer1 = mockk<Peer>()
            peer2 = mockk<Peer>()
            peer3 = mockk<Peer>()
            peer4 = mockk<Peer>()

            ipv4P1 = mockk<IPv4Address>()
            ipv4P2 = mockk<IPv4Address>()
            ipv4P3 = mockk<IPv4Address>()
            ipv4P4 = mockk<IPv4Address>()

            every { peer1.address } returns ipv4P1
            every { peer2.address } returns ipv4P2
            every { peer3.address } returns ipv4P3
            every { peer4.address } returns ipv4P4

            every { ipv4P1.toString() } returns "1"
            every { ipv4P2.toString() } returns "2"
            every { ipv4P3.toString() } returns "3"
            every { ipv4P4.toString() } returns "4"

            mostRecentSWBlock = mockk<TrustChainBlock>()
            proposeBlockData = mockk<SWSignatureAskBlockTD>()
            signatures = emptyList()
            context = mockk<Context>()

            every { mostRecentSWBlock.toString() } returns mostRecentSWBlockArray
            every { proposeBlockData.toString() } returns proposeBlockDataArray
            every { context.toString() } returns contextArray

            every { community.checkLeaderExists(any()) } returns true

            every { community.sendPayload(any(), any()) } just runs

            currentLeader = HashMap()
            currentLeader[daoID.decodeToString()] = peer4

            every { community.getCurrentLeader() } returns currentLeader

            every {
                community.createElectionRequest(
                    any()
                )
            } returns electionPacket

            every { community.leaderSignProposal(any(), any(), any(), any()) } answers { callOriginal() }
        }
    }

    @Test
    fun leaderSingProposalTest() {
        val payload =
            SignPayload(
                "02313685c1912a141279f8248fc8db5899c5df5b".toByteArray(),
                mostRecentSWBlock,
                proposeBlockData,
                signatures
            ).serialize()

        every { community.getPeers() } answers { listOf(peer1, peer3, peer4) }
        every { community.getServiceIdNew() } returns "02313685c1912a141279f8248fc8db5899c5df5b"

        community.leaderSignProposal(mostRecentSWBlock, proposeBlockData, signatures, daoID)

        verify { community.sendPayload(peer1, electionPacket) }
        verify { community.sendPayload(peer3, electionPacket) }
        verify { community.sendPayload(peer4, electionPacket) }

        verify { community.sendPayload(peer4, payload) }

        verify { community.checkLeaderExists(daoID) }
    }
}
