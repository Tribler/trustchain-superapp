package nl.tudelft.trustchain.atomicswap.ui.enums

import nl.tudelft.trustchain.atomicswap.R

enum class TradeOfferStatus(val stringResourceId: Int) {
    OPEN(R.string.status_open),
    OPEN_BY_CURRENT_USER(R.string.status_open),
    IN_PROGRESS(R.string.status_in_progress),
    COMPLETED(R.string.status_completed)
}
