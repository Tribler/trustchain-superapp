package nl.tudelft.trustchain.eurotoken.ui.trustscores

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.ItemTrustscoreBinding
import nl.tudelft.trustchain.eurotoken.entity.TrustScore

/**
 * [TrustScoreItemRenderer] used by the [TrustScoresFragment] to render the [TrustScore] items as a list.
 */
class TrustScoreItemRenderer : ItemLayoutRenderer<TrustScoreItem, View>(
    TrustScoreItem::class.java
) {
    override fun bindView(
        item: TrustScoreItem,
        view: View
    ) = with(view) {
        val binding = ItemTrustscoreBinding.bind(view)
        binding.txtPubKey.text = item.trustScore.pubKey.toHex()
        binding.txtTrustScore.text = item.trustScore.trust.toString() + "%"
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_trustscore
    }
}
