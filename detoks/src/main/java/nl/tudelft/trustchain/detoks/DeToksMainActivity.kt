package nl.tudelft.trustchain.detoks

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import nl.tudelft.trustchain.common.BaseActivity

class DeToksActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_detoks
    var gossipService: GossiperService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar!!.hide()

        Intent(this, GossiperService::class.java).also { intent ->
            startService(intent)
            bindService(intent, gossipConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private val gossipConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as GossiperService.LocalBinder
            gossipService = binder.getService()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            gossipService = null
        }
    }
}
