package com.walkertribe.ian.protocol.udp

/**
 * A discovered server
 * @author rjwut
 */
data class Server internal constructor(
    /**
     * The IP address for this server.
     */
    val ip: String,

    /**
     * The host name for this server.
     */
    val hostName: String
) {
    internal companion object {
        const val ENQ: Byte = 0x05
        const val ACK: Byte = 0x06
    }
}
