package nl.tudelft.trustchain.app

import android.app.Activity
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.example.musicdao.MusicActivity
import nl.tudelft.trustchain.FOC.MainActivityFOC
import nl.tudelft.trustchain.common.R
import nl.tudelft.trustchain.explorer.ui.TrustChainExplorerActivity
import nl.tudelft.trustchain.currencyii.CurrencyIIMainActivity
import nl.tudelft.trustchain.debug.DebugActivity
import nl.tudelft.trustchain.distributedAI.DistributedActivity
import nl.tudelft.trustchain.eurotoken.EuroTokenMainActivity
import nl.tudelft.trustchain.ssi.SSIMainActivity
import nl.tudelft.trustchain.liquidity.LiquidityPoolMainActivity
import nl.tudelft.trustchain.payloadgenerator.ui.TrustChainPayloadGeneratorActivity
import nl.tudelft.trustchain.peerchat.PeerChatActivity
import nl.tudelft.trustchain.trader.ui.TrustChainTraderActivity
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.voting.VotingActivity

enum class AppDefinition(
    @DrawableRes val icon: Int,
    val appName: String,
    @ColorRes val color: Int,
    val activity: Class<out Activity>,
    val disableImageTint: Boolean = false
) {
    EIGHTEEN_PLUS(
        R.drawable.ic_18_plus,
        "18+",
        R.color.red,
        SSIMainActivity::class.java,
        true,
    ),
    PEERCHAT(
        R.drawable.ic_chat_black_24dp,
        "PeerChat",
        R.color.purple,
        PeerChatActivity::class.java
    ),
    TRUSTCHAIN_EXPLORER(
        R.drawable.ic_device_hub_black_24dp,
        "TrustChain Explorer",
        R.color.red,
        TrustChainExplorerActivity::class.java
    ),
    DEBUG(
        R.drawable.ic_bug_report_black_24dp,
        "Debug",
        R.color.dark_gray,
        DebugActivity::class.java
    ),
    CURRENCY_II(
        R.drawable.ic_bitcoin,
        "Luxury Communism",
        R.color.metallic_gold,
        CurrencyIIMainActivity::class.java
    ),
    TRUSTCHAIN_TRADER(
        R.drawable.ic_device_hub_black_24dp,
        "AI trading bot",
        R.color.blue,
        TrustChainTraderActivity::class.java
    ),
    TRUSTCHAIN_PAYLOADGENERATOR(
        R.drawable.ic_add_black_24dp,
        "Market Bot",
        R.color.black,
        TrustChainPayloadGeneratorActivity::class.java
    ),
    FREEDOM_OF_COMPUTING(
        R.drawable.ic_naruto,
        "Freedom of Computing",
        R.color.blue,
        MainActivityFOC::class.java
    ),
    DNA(
        R.drawable.ic_bug_report_black_24dp,
        "Distributed AI",
        R.color.red,
        DistributedActivity::class.java
    ),
    VOTING(
        R.drawable.abc_ic_voice_search_api_material,
        "Voter",
        R.color.android_green,
        VotingActivity::class.java
    ),
    MUSIC_DAO(
        android.R.drawable.ic_media_play,
        "MusicDAO",
        R.color.black,
        MusicActivity::class.java
    ),
    EUROTOKEN(
        R.drawable.ic_baseline_euro_symbol_24,
        "EuroToken",
        R.color.metallic_gold,
        EuroTokenMainActivity::class.java
    ),
    LIQUIDITY(
        R.drawable.ic_pool,
        "Liquidity Pool",
        R.color.blue,
        LiquidityPoolMainActivity::class.java
    ),
    VALUETRANSFER(
        R.drawable.ic_value_transfer,
        "Value Transfer",
        R.color.colorPrimaryValueTransfer,
        ValueTransferMainActivity::class.java
    )
}
