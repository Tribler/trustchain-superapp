package com.example.offlinemoney.ui.trustscores

import com.mattskala.itemadapter.Item
import com.example.offlinemoney.entity.TrustScore

/**
 * [TrustScoreItem] used by the [TrustScoreItemRenderer] to render the list of [TrustScore]s.
 */
class TrustScoreItem(val trustScore: TrustScore) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is TrustScoreItem
            && trustScore.pubKey.contentEquals(other.trustScore.pubKey)
            && trustScore.trust == other.trustScore.trust
    }
}
