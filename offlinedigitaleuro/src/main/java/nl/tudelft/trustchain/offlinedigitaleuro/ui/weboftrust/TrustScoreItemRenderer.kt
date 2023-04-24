package nl.tudelft.trustchain.offlinedigitaleuro.ui.weboftrust

import android.view.View
import androidx.core.content.ContextCompat
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_trustscore.view.*
import nl.tudelft.trustchain.offlinedigitaleuro.R

class TrustScoreItemRenderer : ItemLayoutRenderer<TrustScoreItem, View>(
    TrustScoreItem::class.java
) {

    override fun bindView(item: TrustScoreItem, view: View) = with(view) {
        txtPubKey.text = item.trustScore.public_key
        txtTrustScore.text = item.trustScore.trust_score.toString()
        if (item.trustScore.trust_score <= 0)
            txtTrustScore.setTextColor(ContextCompat.getColor(context, R.color.errorColor))
        else
            if(item.trustScore.trust_score < 30)
                txtTrustScore.setTextColor(ContextCompat.getColor(context, R.color.metallic_gold))
            else
                txtTrustScore.setTextColor(ContextCompat.getColor(context, R.color.green_190))
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_trustscore
    }
}
