package com.example.musicdao

import org.junit.Test
import org.junit.Assert.*
import org.knowm.xchange.Exchange
import org.knowm.xchange.ExchangeFactory
import org.knowm.xchange.binance.BinanceExchange
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.service.marketdata.MarketDataService
import java.math.BigDecimal

class CryptoExchangeTests {
    @Test
    fun testBTCUSDT() {
        val bitstamp: Exchange = ExchangeFactory.INSTANCE.createExchange(BinanceExchange::class.java.name)
        val marketDataService: MarketDataService = bitstamp.marketDataService
        val ticker = marketDataService.getTicker(CurrencyPair.BTC_USDT)
        val conversionRate = BigDecimal(1.0).divide(ticker.ask, 10, 0) * BigDecimal(1_000_000)
        println("$conversionRate mBTC per USDT")
        assertNotNull(ticker.ask)
    }
}
