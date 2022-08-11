package id.walt.services.context

import android.util.Log
import id.walt.servicematrix.BaseService
import id.walt.servicematrix.ServiceProvider
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.hkvstore.HKVKey
import id.walt.services.hkvstore.HKVStoreService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.vcstore.VcStoreService

interface Context {
    val keyStore: KeyStoreService
    val vcStore: VcStoreService
    val hkvStore: HKVStoreService
}

abstract class ContextManager : BaseService() {
    override val implementation: ContextManager get() = serviceImplementation()

    abstract val keyStore: KeyStoreService
    abstract val vcStore: VcStoreService
    abstract val hkvStore: HKVStoreService

    abstract fun <R> runWith(context: Context, action: () -> R): R

    companion object : ServiceProvider {
        val implementation: ContextManager get() = ServiceRegistry.getService()

        override fun getService() = implementation
        override fun defaultImplementation() = WaltIdContextManager()

        // convenience accessors
        val keyStore: KeyStoreService
            get() = implementation.keyStore
        val vcStore: VcStoreService
            get() = implementation.vcStore
        val hkvStore: HKVStoreService
            get() = implementation.hkvStore

        fun <R> runWith(context: Context, action: () -> R): R = implementation.runWith(context, action)
    }
}

open class WaltIdContextManager : ContextManager() {

    val threadContexts: HashMap<Long, Context> = HashMap()

    val currentContext: Context
        get() {
//            Log.e("ContextManager", "Getting current context from $this")
            return WaltIdContext //threadContexts[Thread.currentThread().id] ?: WaltIdContext
            }

    fun setCurrentContext(context: Context) {
        threadContexts[Thread.currentThread().id] = context
    }

    fun resetCurrentContext() {
        threadContexts.remove(Thread.currentThread().id)
    }

    override fun <R> runWith(context: Context, action: () -> R): R {
        try {
            setCurrentContext(context)
            return action.invoke()
        } finally {
            resetCurrentContext()
        }
    }

    override val keyStore: KeyStoreService
        get() = currentContext.keyStore
    override val vcStore: VcStoreService
        get() = currentContext.vcStore
    override val hkvStore: HKVStoreService
        get() = currentContext.hkvStore
}
