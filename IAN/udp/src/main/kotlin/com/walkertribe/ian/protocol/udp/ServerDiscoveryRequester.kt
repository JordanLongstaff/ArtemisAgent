package com.walkertribe.ian.protocol.udp

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.readShortLittleEndian
import io.ktor.utils.io.core.readTextExactCharacters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Searches for servers on the LAN. When run in a coroutine, the requester will broadcast out a
 * request to discover servers, then listen for response for a configurable amount of time. The
 * requester can be reused to send out requests again, which you will need to do if you wish to
 * continually poll for servers.
 * @author rjwut
 */
class ServerDiscoveryRequester(private val listener: Listener, private val timeoutMs: Long) {
    /**
     * Interface for an object which is notified when a server is discovered or the discovery
     * process ends.
     */
    interface Listener {
        /**
         * Invoked when a [Server] is discovered.
         */
        suspend fun onDiscovered(server: Server)

        /**
         * Invoked when the [ServerDiscoveryRequester] quits listening for responses.
         */
        suspend fun onQuit()
    }

    suspend fun run(broadcastAddress: String) {
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
                            val ip = packet.readTextExactCharacters(ipLength)

                            val hostnameLength = packet.readShortLittleEndian().toInt()
                            val hostname = packet.readTextExactCharacters(hostnameLength)
                            listener.onDiscovered(Server(ip, hostname))
                        }
                    } while (true)
                }
            }
        }

        listener.onQuit()
    }

    companion object {
        const val PORT = 3100
        const val DEFAULT_BROADCAST_ADDRESS = "255.255.255.255"
    }

    init {
        require(timeoutMs >= 1) { "Invalid timeout: $timeoutMs" }
    }
}
