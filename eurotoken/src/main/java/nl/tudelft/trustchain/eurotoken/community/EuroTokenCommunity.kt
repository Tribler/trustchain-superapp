package nl.tudelft.trustchain.eurotoken.community

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.eurotoken.ContactItem
import nl.tudelft.trustchain.eurotoken.db.EuroTokenStore
import nl.tudelft.trustchain.eurotoken.entity.Contact
import nl.tudelft.trustchain.eurotoken.entity.Transaction

class EuroTokenCommunity(
    private val database: EuroTokenStore,
    private val context: Context
) : Community() {

    override val serviceId = "5b836c242b793110070f926220c3666ca4c0ea6b"
    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    private val trustchain: TrustChainCommunity by lazy {
        getTrustChainCommunity()
    }

    private val BLOCK_TYPE_TRANSACION = "EuroToken_$serviceId"

    fun getTransactions(): List<Transaction> {
        return trustchain.database.getBlocksWithType(BLOCK_TYPE_TRANSACION).map { block ->
            Transaction(
                block.blockId,
                block.transaction["amount"] as Int,
                AndroidCryptoProvider.keyFromPublicBin(block.publicKey),
                AndroidCryptoProvider.keyFromPublicBin(block.linkPublicKey),
                block.isProposal,
                block.timestamp,
                true,
                false,
                false,
                false
            )
        }
    }

    fun getContactsWithLastTransactions() : List<Pair<Contact, Transaction?>> {

        val contacts = database.getContacts()
        val transactions = getTransactions()
        val notContacts = transactions
            .asSequence()
            .filter { !it.outgoing }
            .map { it.sender }
            .distinct()
            .filter { publicKey -> contacts.find { it.publicKey == publicKey } == null }
            .map { Contact("", it) }
            .toList()

        return (contacts + notContacts).map { contact ->
            val lastMessage = transactions.findLast {
                it.recipient == contact.publicKey || it.sender == contact.publicKey
            }
            Pair(contact, lastMessage)
        }
    }


    fun send_message(peer: Peer) {
        val transaction = mapOf("message" to "hello")
        val publicKey = peer.publicKey.keyToBin()
        trustchain.createProposalBlock(BLOCK_TYPE_TRANSACION, transaction, publicKey)
    }

    class Factory(
        private val database: EuroTokenStore,
        private val context: Context
    ) : Overlay.Factory<EuroTokenCommunity>(EuroTokenCommunity::class.java) {
        override fun create(): EuroTokenCommunity {
            return EuroTokenCommunity(database, context)
        }
    }
}
