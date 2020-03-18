package nl.tudelft.ipv8.android.demo.ui.bitcoin

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

interface BitcoinViewController {
    fun showView(bitcoinViewName: String)
    fun showSharedWalletTransactionView(sharedWalletBlock: TrustChainBlock)
}
