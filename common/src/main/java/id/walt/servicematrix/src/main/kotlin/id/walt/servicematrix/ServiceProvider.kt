package id.walt.servicematrix

import android.content.Context

interface ServiceProvider {
    fun getService(): BaseService

    fun defaultImplementation(): BaseService? = null

    fun defaultImplementation(context: Context): BaseService? = null
}
