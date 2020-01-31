package nl.tudelft.ipv8.android.demo.ui.blocks

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

data class BlockItem(val block: TrustChainBlock): Item()
