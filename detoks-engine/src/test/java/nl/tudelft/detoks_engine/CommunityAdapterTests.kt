package nl.tudelft.detoks_engine

import android.content.Context
import io.mockk.*
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.detoks_engine.db.LastTokenStore
import nl.tudelft.trustchain.detoks_engine.manage_tokens.Transaction
import nl.tudelft.trustchain.detoks_engine.trustchain.CommunityAdapter
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CommunityAdapterTests {

    private lateinit var communityAdapter: CommunityAdapter
    private lateinit var blockHandlerSlot: CapturingSlot<BlockListener>
    private lateinit var trustChainCommunity: TrustChainCommunity
    @Before
    fun init() {
        val context: Context = mockk()
        trustChainCommunity = mockk()
        every {trustChainCommunity.database.getLatest(any(), any())} returns null

        blockHandlerSlot = slot<BlockListener>()
        every {trustChainCommunity.addListener(CommunityAdapter.TOKEN_BLOCK_TYPE, capture(blockHandlerSlot))} returns Unit
        every {trustChainCommunity.myPeer.publicKey.keyToBin()} returns "mypeer".encodeToByteArray()

        mockkObject(LastTokenStore)
        val tokenStore: LastTokenStore = mockk()
        every { LastTokenStore.getInstance(any()) } returns tokenStore
        every { tokenStore.getLastToken()} returns null

        communityAdapter = CommunityAdapter.getInstance(context, trustChainCommunity)
    }

    @After
    fun shutDown() {
        CommunityAdapter.destroy()
    }

    @Test
    fun receive_proposal_test() {
        val transaction = mapOf("test1" to "test2")
        val proposalBlock: TrustChainBlock = mockk()
        every { proposalBlock.isProposal } returns true
        every { proposalBlock.isAgreement } returns false
        every { proposalBlock.isSelfSigned } returns false
        every { proposalBlock.publicKey } returns "bla".encodeToByteArray()
        every { proposalBlock.linkPublicKey } returns "mypeer".encodeToByteArray()
        every { proposalBlock.transaction } returns transaction
        every { proposalBlock.sequenceNumber } returns mockk()

        val agreementBlock: TrustChainBlock = mockk()
        every { agreementBlock.isProposal } returns false
        every { agreementBlock.isAgreement } returns true
        every { agreementBlock.isSelfSigned } returns false
        every { agreementBlock.publicKey } returns "mypeer".encodeToByteArray()
        every { agreementBlock.linkPublicKey } returns "bla".encodeToByteArray()
        every { agreementBlock.transaction } returns transaction
        every { agreementBlock.sequenceNumber } returns mockk()

        every { trustChainCommunity.createAgreementBlock(proposalBlock, transaction) } returns agreementBlock
        blockHandlerSlot.captured.onBlockReceived(proposalBlock)


        verify { trustChainCommunity.createAgreementBlock(proposalBlock, transaction) }

    }



    @Test
    fun receive_own_agreement() {
        val transactionHandler: (transaction: Transaction) -> Unit = mockk()
        communityAdapter.setReceiveTransactionHandler(transactionHandler)

        val transaction = Transaction(listOf("token1", "token2"))

        val agreementBlock: TrustChainBlock = mockk()
        every { agreementBlock.isProposal } returns false
        every { agreementBlock.isAgreement } returns true
        every { agreementBlock.isSelfSigned } returns false
        every { agreementBlock.publicKey } returns "mypeer".encodeToByteArray()
        every { agreementBlock.linkPublicKey } returns "bla".encodeToByteArray()
        every { agreementBlock.transaction } returns transaction.toTrustChainTransaction()
        every { agreementBlock.sequenceNumber.toInt() } returns 1

        val oldTokenCount = communityAdapter.getTokenCount()

        every { transactionHandler.invoke(any()) } returns Unit
        blockHandlerSlot.captured.onBlockReceived(agreementBlock)

        val transactionSlot = slot<Transaction>()

        verify { transactionHandler.invoke(capture(transactionSlot)) }


        Assert.assertArrayEquals(arrayOf(transactionSlot.captured.tokens), arrayOf(transaction.tokens))
        Assert.assertEquals(transaction.transactionId, transactionSlot.captured.transactionId)
        Assert.assertEquals(oldTokenCount + 2, communityAdapter.getTokenCount())



    }
}
