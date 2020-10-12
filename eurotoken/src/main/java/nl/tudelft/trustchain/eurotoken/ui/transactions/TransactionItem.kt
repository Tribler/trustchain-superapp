package nl.tudelft.trustchain.eurotoken.ui.transactions

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

data class TransactionItem (
        val block: TrustChainBlock
    ) : Item()
