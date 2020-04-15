package nl.tudelft.trustchain.app

import android.app.Activity
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import nl.tudelft.trustchain.FOC.MainActivityFOC
import nl.tudelft.trustchain.common.R
import nl.tudelft.trustchain.currencyii.MainActivity
import nl.tudelft.trustchain.debug.DebugActivity
import nl.tudelft.trustchain.explorer.ui.TrustChainExplorerActivity
import nl.tudelft.trustchain.trader.ui.TrustChainTraderActivity
import nl.tudelft.trustchain.payloadgenerator.ui.TrustChainPayloadGeneratorActivity
import nl.tudelft.trustchain.voting.VotingActivity

enum class AppDefinition(
    @DrawableRes val icon: Int,
    val appName: String,
    @ColorRes val color: Int,
    val activity: Class<out Activity>
) {
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
    TRUSTCHAIN_TRADER(
        R.drawable.ic_device_hub_black_24dp,
        "Trader",
        R.color.blue,
        TrustChainTraderActivity::class.java
    ),
    TRUSTCHAIN_PAYLOADGENERATOR(
        R.drawable.ic_add_black_24dp,
        "Market Bot",
        R.color.black,
        TrustChainPayloadGeneratorActivity::class.java
    ),
    CURRENCYII(
        R.drawable.ic_attach_money_black_24dp,
        "Currency",
        R.color.blue,
        MainActivity::class.java
    ),
    FOC(
        R.drawable.ic_naruto,
        "FOC",
        R.color.blue,
        MainActivityFOC::class.java
    ),
    VOTING(
        R.drawable.abc_ic_voice_search_api_material,
        "Voting",
        R.color.purple,
        VotingActivity::class.java
    )
}
