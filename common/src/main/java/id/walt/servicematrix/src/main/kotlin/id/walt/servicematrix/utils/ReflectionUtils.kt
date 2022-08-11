@file:Suppress("UNCHECKED_CAST")

package id.walt.servicematrix.utils

import id.walt.servicematrix.BaseService
import kotlin.reflect.KClass

/**
 * General reflection utilities used by waltid-servicematrix
 */
object ReflectionUtils {
    /**
     * @return KClass of the specified class by full name
     */
    fun getKClassByName(name: String): KClass<out BaseService> {
        return Class.forName(name).kotlin as KClass<out BaseService>
    }
}
