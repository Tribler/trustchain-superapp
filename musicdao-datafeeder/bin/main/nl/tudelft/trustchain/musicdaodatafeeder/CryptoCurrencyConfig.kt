package nl.tudelft.trustchain.musicdaodatafeeder

import org.bitcoinj.params.RegTestParams

object CryptoCurrencyConfig {
    val networkParams: RegTestParams = RegTestParams.get()
    const val chainFileName: String = "forwarding-service-regtest"
}
