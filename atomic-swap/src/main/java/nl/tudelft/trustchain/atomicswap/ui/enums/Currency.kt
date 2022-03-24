package nl.tudelft.trustchain.atomicswap.ui.enums

import nl.tudelft.trustchain.atomicswap.R

enum class Currency(val currencyCodeStringResourceId: Int) {
    BITCOIN(R.string.currency_code_bitcoin),
    ETHEREUM(R.string.currency_code_ethereum)
}
