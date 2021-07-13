package com.walkertribe.ian.protocol.udp

import java.io.IOException
import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface

/**
 * A class which contains all the information needed to perform and respond to
 * UDP server discovery broadcasts.
 * @author rjwut
 */
class PrivateNetworkAddress private constructor(
    private val networkInterface: NetworkInterface,
    addr: InterfaceAddress
) {
    /**
     * Returns the InetAddress.
     */
    val inetAddress: InetAddress? = addr.address

    /**
     * Returns the broadcast address.
     */
    val broadcastAddress: InetAddress? = addr.broadcast

    override fun toString(): String =
        "${networkInterface.displayName} [${networkInterface.name}]"

    companion object {
        /**
         * Returns a PrivateNetworkAddress believed to represent the best one to
         * represent this machine on the LAN, or null if none can be found.
         */
        @Throws(IOException::class)
        fun guessBest(): PrivateNetworkAddress? = findAll().firstOrNull {
            it.inetAddress != null && it.broadcastAddress != null
        }

        /**
         * Returns a prioritized list of PrivateNetworkAddress objects.
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
                    iface.interfaceAddresses.forEach { ifaceAddr ->
                        val addr = ifaceAddr.address
                        val type = PrivateNetworkType(addr.address)
                        if (type != null) {
                            list.add(PrivateNetworkAddress(iface, ifaceAddr))
                        }
                    }
                }
                list
            }.orEmpty()
    }
}
