package nl.tudelft.trustchain.offlinedigitaleuro.ui

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_trustscore.view.*
import nl.tudelft.trustchain.offlinedigitaleuro.R

class TrustScoreItemRenderer : ItemLayoutRenderer<TrustScoreItem, View>(
    TrustScoreItem::class.java
) {

    override fun bindView(item: TrustScoreItem, view: View) = with(view) {
        txtPubKey.text = item.trustScore.public_key
        txtTrustScore.text = item.trustScore.trust_score.toString()
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_trustscore
    }
}
