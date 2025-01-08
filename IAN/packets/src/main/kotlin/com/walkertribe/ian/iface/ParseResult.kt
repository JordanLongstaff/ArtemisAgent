package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketException
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacket

/**
 * Object which reports the results of a packet parsing attempt.
 *
 * @author rjwut
 */
sealed class ParseResult {
    internal class Processing : ParseResult()

    data object Skip : ParseResult() {
        override fun addListeners(listeners: Iterable<ListenerModule>) {
            error("Cannot add listeners; packet is not recognized")
        }
    }

    class Success(
        /** Returns the packet object that was parsed. */
        val packet: Packet.Server,
        prevResult: ParseResult,
    ) : ParseResult() {
        init {
            addListeners(prevResult.interestedListeners)
        }

        fun fireListeners() {
            interestedListeners.forEach(packet::offerTo)
        }
    }

    data class Fail(
        /**
         * Return any exception that occurred while parsing the packet. This is only for non-fatal
         * exceptions. A fatal exception (one occurring before the payload can be read) should cause
         * the exception to be thrown instead.
         */
        val exception: PacketException
    ) : ParseResult() {
        override fun addListeners(listeners: Iterable<ListenerModule>) {
            error("Cannot add listeners; parsing failed")
        }
    }

    protected val interestedListeners: MutableSet<ListenerModule> = mutableSetOf()

    /**
     * Adds [ListenerModule]s that are interested in the packet, or any objects in an
     * [ObjectUpdatePacket].
     */
    open fun addListeners(listeners: Iterable<ListenerModule>) {
        interestedListeners.addAll(listeners)
    }

    /**
     * Returns true if the packet was of interest to any listeners. Note that in the case of an
     * [ObjectUpdatePacket], there may be listeners that aren't interested in the packet itself, but
     * are interested in certain types of objects the packet may contain.
     */
    val isInteresting: Boolean
        get() = interestedListeners.isNotEmpty()
}
