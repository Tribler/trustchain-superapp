package nl.tudelft.trustchain.offlinedigitaleuro.ui.weboftrust

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.offlinedigitaleuro.db.WebOfTrust

class TrustScoreItem(val trustScore: WebOfTrust) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is TrustScoreItem
            && trustScore.public_key.contentEquals(other.trustScore.public_key)
            && trustScore.trust_score == other.trustScore.trust_score
    }
}
