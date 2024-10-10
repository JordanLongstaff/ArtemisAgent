package com.walkertribe.ian.protocol.udp

import kotlinx.io.IOException
import java.net.NetworkInterface

/**
 * A class which contains all the information needed to perform and respond to UDP server discovery
 * broadcasts.
 * @author rjwut
 */
class PrivateNetworkAddress private constructor(
    /**
     * Returns the broadcast address.
     */
    val hostAddress: String
) {
    companion object {
        private const val DEFAULT_HOST = "255.255.255.255"

        val DEFAULT by lazy { PrivateNetworkAddress(DEFAULT_HOST) }

        /**
         * Returns a [PrivateNetworkAddress] believed to represent the best one to represent this
         * machine on the LAN, or null if none can be found.
         */
        @Throws(IOException::class)
        fun guessBest(): PrivateNetworkAddress? = findAll().firstOrNull()

        /**
         * Returns a prioritized list of valid [PrivateNetworkAddress]es.
         */
        @Throws(IOException::class)
        fun findAll(): List<PrivateNetworkAddress> =
            NetworkInterface.getNetworkInterfaces()?.let { ifaces ->
                val list: MutableList<PrivateNetworkAddress> = mutableListOf()
                while (ifaces.hasMoreElements()) {
                    val iface = ifaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) {
                        continue // we don't want loopback interfaces or interfaces that are down
                    }
                    list.addAll(
                        iface.interfaceAddresses.mapNotNull { ifaceAddr ->
                            val addr = ifaceAddr?.address ?: return@mapNotNull null
                            if (PrivateNetworkType(addr.address) == null) return@mapNotNull null
                            ifaceAddr.broadcast?.let { PrivateNetworkAddress(it.hostAddress) }
                        }
                    )
                }
                list
            }.orEmpty()
    }
}
