package com.walkertribe.ian.protocol

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.ListenerArgument
import com.walkertribe.ian.iface.PacketWriter

/**
 * Interface for all packets that can be received or sent.
 */
interface ArtemisPacket : ListenerArgument {
    /**
     * Returns an Origin value indicating the type of connection from which
     * this packet originates. SERVER means that this packet type is sent by
     * the server; CLIENT means it's sent by the client.
     */
    val origin: Origin

    /**
     * Returns the type value for this packet, specified as the last field of
     * the preamble.
     */
    val type: Int

    /**
     * Writes this packet to the given PacketWriter, then returns the array of
     * bytes that was sent.
     */
    suspend fun writeTo(writer: PacketWriter)

    companion object {
        /**
         * The preamble of every packet starts with this value.
         */
        const val HEADER = 0xDEADBEEF.toInt()
        const val PREAMBLE_SIZE = Int.SIZE_BYTES * 6
    }
}
