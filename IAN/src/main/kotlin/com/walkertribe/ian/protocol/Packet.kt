package com.walkertribe.ian.protocol

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.util.JamCrc

/**
 * Annotation for packet classes. This annotation is not inherited.
 * @author rjwut
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class Packet(
    /**
     * Whether the packet originates from the server or the client. This value
     * is required.
     */
    val origin: Origin,
    /**
     * The packet type, given as a string, which is then JamCRC hashed. You
     * must specify either this or hash.
     */
    val type: String = "",
    /**
     * The packet type, given as a JamCRC hash value. You must specify either
     * this or type.
     */
    val hash: Int = 0,
    /**
     * Optional packet subtype values. If you specify more than one subtype,
     * this class will parse packets with any of the given subtypes.
     */
    val subtype: Byte = -1
)

/**
 * Determines the packet type hash specified in the given Packet annotation.
 */
internal fun Packet.getHash(): Int {
    val typeName = type
    if (typeName.isNotEmpty()) {
        return JamCrc.compute(typeName)
    }
    val type = hash
    if (type == 0) {
        throw ArtemisPacketException("@Packet must have either a type or a hash")
    }
    return type
}
