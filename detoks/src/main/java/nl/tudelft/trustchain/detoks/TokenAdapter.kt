package nl.tudelft.trustchain.detoks

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * Adapter class to create list items for the token list
 */
class TokenAdapter(private val context: Activity, private val tokenArray: ArrayList<Token>) : ArrayAdapter<Token>(context, R.layout.list_item, tokenArray){

    private var groupAmount = 10
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.list_item, null, true)

        val token = getItem(position)
        val record = rowView.findViewById(R.id.item_number) as TextView
        val token_id = rowView.findViewById(R.id.item_id) as TextView

        record.text = "Group:" + (token.tokenIntId / groupAmount) + " Token ID: " + token.tokenIntId
        token_id.text = "ID: " + token.unique_id
        return rowView
    }

    /**
     * Retrieves item at position
     * @param position: position of item
     */
    override fun getItem(position: Int): Token {
        return tokenArray[position]
    }

    fun setGroupSize(groupAmount : Int){
        this.groupAmount = groupAmount
    }
}
