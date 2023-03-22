package nl.tudelft.trustchain.detoks

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.TextView
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.util.toHex

class BlockAdapter (private val context: Activity, private val blockArray: ArrayList<TrustChainBlock>) : ArrayAdapter<TrustChainBlock>(context, R.layout.list_item, blockArray){

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.list_item, null, true)

        val block = getItem(position)
        val blockType = rowView.findViewById(R.id.block_type) as TextView
        val record = rowView.findViewById(R.id.record_number) as TextView
        val block_number = rowView.findViewById(R.id.block_number) as TextView
        val sender = rowView.findViewById(R.id.sender_id) as TextView
        val receiver = rowView.findViewById(R.id.receiver_id) as TextView

        blockType.text = if (block.isProposal) "Proposal" else "Agreement"
        record.text = "Record: " + position.toString()
        block_number.text = "Block: " + block.blockId.toString()
        sender.text = "Sender: " + block.publicKey.toHex()
        receiver.text = "Receiver: " + block.linkPublicKey.toHex()
        return rowView
    }

    override fun getItem(position: Int): TrustChainBlock {
        return blockArray[position]
    }
}
