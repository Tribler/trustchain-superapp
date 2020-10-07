package com.example.musicdao.dialog

import org.junit.Assert
import org.junit.Test

class TipArtistDialogTest {
    private val errorText = "USD (? mBTC)"
    private val dialog = TipArtistDialog("pk")

    @Test
    fun invalidConversionRate() {
        val invalid = "abc"
        Assert.assertEquals(
            errorText,
            dialog.getConversionRate(invalid)
        )
    }

    @Test
    fun validConversionRate() {
        val valid = "100"
        Assert.assertNotEquals(
            errorText,
            dialog.getConversionRate(valid)
        )
    }

    @Test
    fun oneUSDInCrypto() {
        val value = dialog.oneUSDInCrypto()
        Assert.assertNotNull(value)
    }
}
