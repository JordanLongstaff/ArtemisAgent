package com.walkertribe.ian.protocol

/**
 * Thrown when IAN encounters a problem while attempting to read a packet of a known type. Unknown
 * packets don't throw this exception; IAN creates objects for them.
 */
class PacketException
private constructor(
    message: String?,
    cause: Throwable?,
    packetType: Int = 0,
    payload: ByteArray? = null,
) : Exception(message ?: cause?.let { it.message ?: it::class.simpleName }, cause) {
    /** The type value for this packet, or 0 if unknown. */
    var packetType: Int = packetType
        private set

    /** The payload for this packet, or null if unknown. */
    var payload: ByteArray? = payload
        private set

    /**
     * @param message The exception's message.
     * @constructor Constructs a [PacketException] with a message.
     */
    constructor(message: String) : this(message, null)

    /**
     * @param cause The cause of this [PacketException].
     * @constructor Constructs a [PacketException] caused by another exception.
     */
    constructor(cause: Throwable? = null) : this(null, cause)

    /**
     * @param cause The exception that caused PacketException to be thrown
     * @param packetType The packet's type value
     * @param payload The packet's payload bytes
     */
    constructor(
        cause: Throwable?,
        packetType: Int,
        payload: ByteArray?,
    ) : this(null, cause, packetType, payload)

    /**
     * @param message A description of the problem
     * @param packetType The packet's type value
     * @param payload The packet's payload bytes
     */
    constructor(
        message: String?,
        packetType: Int,
        payload: ByteArray?,
    ) : this(message, null, packetType, payload)

    /** Adds the packet type and payload to this exception. */
    fun appendParsingDetails(packetType: Int, payload: ByteArray) {
        this.packetType = packetType
        this.payload = payload
    }

    private companion object {
        @Suppress("ConstPropertyName") private const val serialVersionUID = 6305993950844264082L
    }
}
