package com.walkertribe.ian.protocol.udp

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.readShortLittleEndian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Searches for servers on the LAN. When run on a Thread, the requester will
 * broadcast out a request to discover servers, then listen for response for a
 * configurable amount of time. The requester can be reused to send out
 * requests again, which you will need to do if you wish to continually poll
 * for servers.
 * @author rjwut
 */
class ServerDiscoveryRequester(
    private val broadcastAddress: String =
        PrivateNetworkAddress.guessBest()?.broadcastAddress?.hostAddress ?: DEFAULT_HOST,
    private val listener: Listener,
    private val timeoutMs: Long
) {
    /**
     * Interface for an object which is notified when a server is discovered or
     * the discovery process ends.
     */
    interface Listener {
        /**
         * Invoked when a server is discovered.
         */
        suspend fun onDiscovered(server: Server)

        /**
         * Invoked with the ServerDiscoveryRequester quits listening for responses.
         */
        suspend fun onQuit()
    }

    suspend fun run() {
        SelectorManager(Dispatchers.IO).use { selector ->
            aSocket(selector).udp().bind {
                broadcast = true
            }.use { socket ->
                socket.send(
                    Datagram(
                        packet = buildPacket { writeByte(Server.ENQ) },
                        address = InetSocketAddress(broadcastAddress, PORT)
                    )
                )

                withTimeoutOrNull(timeoutMs) {
                    do {
                        val datagram = socket.receive()
                        val packet = datagram.packet

                        val ack = packet.readByte()
                        if (ack == Server.ACK) {
                            // only accept data starting with ACK
                            val ipLength = packet.readShortLittleEndian().toInt()
                            val ip = packet.readTextExact(ipLength)

                            val hostnameLength = packet.readShortLittleEndian().toInt()
                            val hostname = packet.readTextExact(hostnameLength)
                            listener.onDiscovered(Server(ip, hostname))
                        }
                    } while (true)
                }
            }
        }

        listener.onQuit()
    }

    companion object {
        private const val DEFAULT_HOST = "255.255.255.255"
        const val PORT = 3100
    }

    init {
        require(timeoutMs >= 1) { "Invalid timeout: $timeoutMs" }
    }
}
