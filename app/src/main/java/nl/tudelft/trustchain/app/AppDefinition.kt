package nl.tudelft.trustchain.app

import android.app.Activity
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import nl.tudelft.trustchain.musicdao.MusicActivity
import nl.tudelft.trustchain.FOC.MainActivityFOC
import nl.tudelft.trustchain.common.R
import nl.tudelft.trustchain.currencyii.CurrencyIIMainActivity
import nl.tudelft.trustchain.debug.DebugActivity
import nl.tudelft.trustchain.eurotoken.EuroTokenMainActivity
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.peerai.PeerAIActivity

enum class AppDefinition(
    @DrawableRes val icon: Int,
    val appName: String,
    @ColorRes val color: Int,
    val activity: Class<out Activity>,
    val disableImageTint: Boolean = false,
) {
    DEBUG(
        R.drawable.ic_bug_report_black_24dp,
        "Debug",
        R.color.dark_gray,
        DebugActivity::class.java
    ),
    VALUETRANSFER(
        R.drawable.ic_idelft_logo,
        "IDelft",
        R.color.colorPrimaryValueTransfer,
        ValueTransferMainActivity::class.java,
        true,
    ),
    EUROTOKEN(
        R.drawable.ic_baseline_euro_symbol_24,
        "EuroToken",
        R.color.metallic_gold,
        EuroTokenMainActivity::class.java
    ),
    MUSIC_DAO(
        android.R.drawable.ic_media_play,
        "MusicDAO",
        R.color.black,
        MusicActivity::class.java
    ),
    CURRENCY_II(
        R.drawable.ic_bitcoin,
        "Decentralised Democracy Template",
        R.color.metallic_gold,
        CurrencyIIMainActivity::class.java,
        true,
    ),
    FREEDOM_OF_COMPUTING(
        R.drawable.ic_naruto,
        "Freedom of Computing",
        R.color.blue,
        MainActivityFOC::class.java
    ),
    PeerAi(
        R.drawable.ic_book_hover_over_hand,
        "PeerAI",
        R.color.green,
        PeerAIActivity::class.java
    )
}
