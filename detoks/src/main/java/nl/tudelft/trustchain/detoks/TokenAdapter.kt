package nl.tudelft.trustchain.detoks

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
class TokenAdapter(private val context: Activity, private val tokenArray: ArrayList<nl.tudelft.trustchain.detoks.Token>) : ArrayAdapter<Token>(context, R.layout.list_item, tokenArray){

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.list_item, null, true)

        val token = getItem(position)
        val record = rowView.findViewById(R.id.item_number) as TextView
        val token_id = rowView.findViewById(R.id.item_id) as TextView
        val public_key = rowView.findViewById(R.id.item_field1) as TextView

        public_key.text = token.public_key.toString()
        record.text = "Token: " + position.toString()
        token_id.text = "ID: " + token.unique_id
        return rowView
    }

    override fun getItem(position: Int): Token {
        return tokenArray[position]
    }
}
