package nl.tudelft.ipv8.android.peerdiscovery

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import mu.KotlinLogging
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.peerdiscovery.strategy.DiscoveryStrategy
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * A strategy to perform network service discovery (NSD) on LAN without using a bootstrap server.
 */
class NetworkServiceDiscovery(
    private val nsdManager: NsdManager,
    private val overlay: Overlay
) : DiscoveryStrategy {
    private var serviceName: String? = null

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            logger.info { "Service registered: $serviceInfo" }
            serviceName = serviceInfo.serviceName
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Registration failed! Put debugging code here to determine why.
            logger.error { "Service registration failed: $errorCode" }
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            // Service has been unregistered. This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
            logger.info { "Service unregistered: $serviceInfo" }
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Unregistration failed. Put debugging code here to determine why.
            logger.error { "Service unregistration failed: $errorCode" }
        }
    }

    // Instantiate a new DiscoveryListener
    private val discoveryListener = object : NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            logger.debug { "Service discovery started" }
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            // A service was found! Do something with it.
            logger.debug { "Service found: $serviceInfo" }

            if (serviceInfo.serviceType == SERVICE_TYPE) {
                // This is IPv8 service

                if (serviceInfo.serviceName == serviceName) {
                    logger.debug { "Found its own service" }
                }

                val serviceId = getServiceId(serviceInfo.serviceName)

                logger.debug { "Service ID: $serviceId" }

                if (serviceId == overlay.serviceId) {
                    nsdManager.resolveService(serviceInfo, createResolveListener())
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            logger.debug { "Service lost: $service" }
        }

        override fun onDiscoveryStopped(serviceType: String) {
            logger.debug("Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            logger.error("Discovery failed: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            logger.error { "Discovery failed: $errorCode" }
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Called when the resolve fails. Use the error code to debug.
                logger.error("Resolve failed: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                logger.info("Service resolved: $serviceInfo")

                val peer = overlay.myPeer
                val address = Address(serviceInfo.host.hostAddress, serviceInfo.port)

                if (overlay.myEstimatedLan != address) {
                    logger.debug { "Discovered address: $address" }
                    overlay.network.discoverAddress(peer, address, overlay.serviceId)
                } else {
                    logger.debug { "Resolved its own IP address" }
                }
            }
        }
    }

    private fun registerService(port: Int, serviceName: String) {
        val serviceInfo = NsdServiceInfo()
        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.serviceName = serviceName
        serviceInfo.serviceType = SERVICE_TYPE
        serviceInfo.port = port

        logger.debug { "Registering service info $serviceInfo" }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun discoverServices() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun unregisterService() {
        nsdManager.unregisterService(registrationListener)
    }

    private fun stopServiceDiscovery() {
        nsdManager.stopServiceDiscovery(discoveryListener)
    }

    override fun load() {
        logger.debug { "NetworkServiceDiscovery load" }

        val endpoint = overlay.endpoint
        if (endpoint is UdpEndpoint) {
            val socketPort = endpoint.getSocketPort()
            val serviceName = overlay.serviceId + "_" + Random.nextInt(10000)
            logger.debug { "Registering service $serviceName on port $socketPort" }
            registerService(socketPort, serviceName)
        } else {
            logger.error { "Overlay endpoint is not UdpEndpoint" }
        }
        discoverServices()
    }

    override fun takeStep() {
        val addresses = overlay.getWalkableAddresses()
        if (addresses.isNotEmpty()) {
            val address = addresses.random()
            overlay.walkTo(address)
        }
    }

    override fun unload() {
        logger.debug { "NetworkServiceDiscovery unload" }
        unregisterService()
        stopServiceDiscovery()
    }

    /**
     * The service ID are the first 40 characters of the service name. Returns null if the service
     * name name is invalid.
     */
    private fun getServiceId(serviceName: String): String? {
        // Service ID string length is two times its size in bytes as it is hexadecimal encoded
        val serviceIdLength = Packet.SERVICE_ID_SIZE * 2
        if (serviceName.length < serviceIdLength) return null

        val serviceId = serviceName.substring(0, serviceIdLength)
        for (char in serviceId) {
            if (!char.isDigit() && char !in 'a'..'f') {
                return null
            }
        }

        return serviceId
    }

    companion object {
        private const val SERVICE_TYPE = "_ipv8._udp."
    }

    class Factory(
        private val nsdManager: NsdManager
    ) : DiscoveryStrategy.Factory<NetworkServiceDiscovery>() {
        override fun create(): NetworkServiceDiscovery {
            return NetworkServiceDiscovery(nsdManager, getOverlay())
        }
    }
}
