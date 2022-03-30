package nl.tudelft.trustchain.eurotoken.ui.trustscores

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.eurotoken.entity.TrustScore

class TrustScoreItem(val trustScore: TrustScore) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is TrustScoreItem
            && trustScore.pubKey == other.trustScore.pubKey
            && trustScore.trust == other.trustScore.trust
    }
}
