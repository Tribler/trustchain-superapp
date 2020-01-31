package nl.tudelft.ipv8.android.demo.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.fragment.app.Fragment

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import nl.tudelft.ipv8.android.demo.service.Ipv8Service

abstract class BaseFragment : Fragment() {
    protected var service: Ipv8Service? = null

    protected val uiScope = CoroutineScope(Dispatchers.Main)

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            val binder: Ipv8Service.LocalBinder = iBinder as Ipv8Service.LocalBinder
            service = binder.service
            onServiceConnected(binder.service)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            service = null
        }
    }

    override fun onStart() {
        super.onStart()

        val serviceIntent = Intent(context, Ipv8Service::class.java)
        context?.startService(serviceIntent)
        context?.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        context?.unbindService(serviceConnection)
        super.onStop()
    }

    override fun onDestroyView() {
        uiScope.cancel()
        super.onDestroyView()
    }

    abstract fun onServiceConnected(service: Ipv8Service)
}
