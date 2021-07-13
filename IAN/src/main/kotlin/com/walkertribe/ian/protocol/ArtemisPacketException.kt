package com.walkertribe.ian.protocol

import com.walkertribe.ian.enums.Origin
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readBytes

/**
 * Thrown when IAN encounters a problem while attempting to read or write a
 * packet of a known type. Unknown packets don't throw this exception; IAN
 * creates objects for them.
 */
class ArtemisPacketException private constructor(
    string: String?,
    t: Throwable?,
    origin: Origin? = null,
    packetType: Int = 0,
    payload: ByteReadPacket? = null
) : Exception(string ?: t?.message, t) {
    /**
     * Returns the packet's Origin, or null if unknown.
     */
    var origin: Origin? = origin
        private set

    /**
     * Returns the type value for this packet, or 0 if unknown.
     */
    var packetType: Int = packetType
        private set

    /**
     * Returns the payload for this packet, or null if unknown.
     */
    var payload: ByteReadPacket? = payload
        private set

    /**
     * @param string A description of the problem
     */
    constructor(string: String) : this(string, null)

    /**
     * @param t The exception that caused ArtemisPacketException to be thrown
     */
    constructor(t: Throwable? = null) : this(t, null, 0, null)

    /**
     * @param t The exception that caused ArtemisPacketException to be thrown
     * @param origin The packet's Origin
     * @param packetType The packet's type value
     * @param payload The packet's payload bytes
     */
    constructor(
        t: Throwable?,
        origin: Origin?,
        packetType: Int,
        payload: ByteReadPacket?
    ) : this(null, t, origin, packetType, payload)

    /**
     * @param string A description of the problem
     * @param origin The packet's Origin
     * @param packetType The packet's type value
     * @param payload The packet's payload bytes
     */
    constructor(
        string: String?,
        origin: Origin?,
        packetType: Int,
        payload: ByteReadPacket?
    ) : this(string, null, origin, packetType, payload)

    /**
     * Adds the Origin, packet type and payload to this exception.
     */
    fun appendParsingDetails(origin: Origin, packetType: Int, payload: ByteReadPacket) {
        this.origin = origin
        this.packetType = packetType
        this.payload = payload
    }

    /**
     * Convert the data in this exception to an UnknownPacket. An
     * IllegalStateException will occur if the Origin or payload is null.
     */
    fun toUnknownPacket(): RawPacket.Unknown = RawPacket.Unknown(
        checkNotNull(origin) { "Unknown origin" },
        packetType,
        checkNotNull(payload?.readBytes()) { "Unknown payload" }
    )

    private companion object {
        const val serialVersionUID = 6305993950844264082L
    }
}
