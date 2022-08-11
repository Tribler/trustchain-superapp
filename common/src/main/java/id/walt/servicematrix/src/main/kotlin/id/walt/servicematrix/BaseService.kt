package id.walt.servicematrix

import android.content.Context
import com.sksamuel.hoplite.ConfigLoader
import nl.tudelft.trustchain.common.ebsi.WaltIdInterface

/**
 * Tag data classes with this interface to use it as configuration for your service
 */
interface ServiceConfiguration

/**
 * Base class that a service has to inherit from
 */
abstract class BaseService {
    /**
     * override using your custom ServiceConfiguration and get it through
     * `= fromConfiguration<YourCustomServiceConfig>(configPath)`
     * configPath should be taken from a primary constructor with a single string argument
     */
    open val configuration: ServiceConfiguration?
        get() = error("You have not defined a configuration for this service.")

    /**
     * override with your own Service, inject with
     * `= serviceImplementation<YourCustomService>()`
     */
    abstract val implementation: BaseService

    /**
     * Wrapper around ServiceRegistry.getService()
     * @see ServiceRegistry.getService
     */
    inline fun <reified Service : BaseService> serviceImplementation() = ServiceRegistry.getService<Service>()

    /**
     * Inject configuration
     * @param configurationPath File path to the service configuration, should be injected using the primary constructor
     * @see configuration
     */
    protected inline fun <reified T : ServiceConfiguration> fromConfiguration(configurationPath: String) =
        ConfigLoader().loadConfigOrThrow<T>(configurationPath)

    open lateinit var androidContext: Context
    open val waltIdInterface by lazy {
        WaltIdInterface(androidContext)
    }
}
