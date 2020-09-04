package com.example.musicdao.wallet

import org.bitcoinj.params.RegTestParams

object CryptoCurrencyConfig {
    val networkParams: RegTestParams = RegTestParams.get()
    val chainFileName: String = "forwarding-service-regtest"

    fun getAddress() {

    }
}
