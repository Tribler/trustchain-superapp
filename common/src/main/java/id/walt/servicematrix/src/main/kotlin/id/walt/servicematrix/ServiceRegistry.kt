package id.walt.servicematrix

import android.util.Log
import id.walt.servicematrix.exceptions.UnimplementedServiceException
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

/**
 * Mapping of services and their respective service-implementations
 */
object ServiceRegistry {
    /**
     * Actual mapping happens here, you can directly access the map too.
     */
    val services = HashMap<KClass<out BaseService>, BaseService>()

    /**
     * Register a implementation for a specific service
     * Example: `registerService<MyCustomService>(MyCustomImplementation())`
     */
    inline fun <reified T : BaseService> registerService(serviceImplementation: BaseService) {
        services[T::class] = serviceImplementation
    }

    /**
     * Register an implementation for a specific service
     * Example: `registerService(MyCustomImplementation(), MyCustomService::class)`
     */
    fun registerService(serviceImplementation: BaseService, serviceType: KClass<out BaseService>) {
        services[serviceType] = serviceImplementation
    }

    /**
     * Get the current service implementation for this base service from the service registry
     * Example: `getService<MyCustomService>()`
     */
    inline fun <reified Service : BaseService> getService(): Service {
        return getService(Service::class)
    }

    /**
     * Get the current service implementation for this base service from the service registry
     * Example: `getService(MyCustomService::class)`
     */
    @Suppress("UNCHECKED_CAST")
    fun <Service : BaseService> getService(serviceClass: KClass<Service>): Service {
        services.forEach {
        }

        if (services[serviceClass] != null) {
            return services[serviceClass] as Service
        } else {
            if (serviceClass.companionObjectInstance != null) {
                val compObjService = ((serviceClass.companionObjectInstance as ServiceProvider).defaultImplementation()?.also {
                    registerService(it, serviceClass)
                }) as Service?

                if (compObjService != null) {
                    return compObjService
                } else {
                    throw UnimplementedServiceException(
                        serviceClass.qualifiedName, "and no default service was defined in ServiceProvider"
                    )
                }
            } else {
                throw UnimplementedServiceException(
                    serviceClass.qualifiedName,
                    "and no ServiceProvider was defined for the service?"
                )
            }
        }

        /*return (services[serviceClass]
             ?: (((serviceClass.companionObjectInstance
                ?: throw UnimplementedServiceException(
                    serviceClass.qualifiedName,
                    "and no ServiceProvider was defined for the service?"
                )) as ServiceProvider)

                .defaultImplementation()?.also { registerService(it, serviceClass) }

                ?: throw UnimplementedServiceException(
                    serviceClass.qualifiedName, "and no default service was defined in ServiceProvider"
                ))) as Service*/
    }
}
