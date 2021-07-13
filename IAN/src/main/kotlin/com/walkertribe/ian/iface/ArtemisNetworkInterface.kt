package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.Protocol
import com.walkertribe.ian.util.Version

/**
 * Interface for objects which can connect to an Artemis server and send and
 * receive packets.
 */
interface ArtemisNetworkInterface {
    /**
     * Returns the version of Artemis supported by this interface.
     */
    val version: Version

    /**
     * Registers the packet types defined by the given Protocol with this object. The
     * [CoreArtemisProtocol][com.walkertribe.ian.protocol.core.CoreArtemisProtocol] is registered
     * automatically.
     */
    fun registerProtocol(protocol: Protocol)

    /**
     * Registers an object as a listener. It must have one or more qualifying
     * methods annotated with [Listener].
     */
    fun addListener(listener: Any)

    /**
     * Sets whether heartbeat packets should be sent to the remote machine automatically. Defaults
     * to true. Set this to false if you pass this object to another interface's proxyTo() method
     * and don't capture heartbeat packets in any of your listeners.
     */
    fun setAutoSendHeartbeat(autoSendHeartbeat: Boolean)

    /**
     * Sets the timeout value for listening for heartbeat packets.
     */
    fun setTimeout(timeout: Long)

    /**
     * Opens the send/receive streams to the remote machine.
     */
    fun start()

    /**
     * Returns true if currently connected to the remote machine; false
     * otherwise.
     */
    val isConnected: Boolean

    /**
     * Enqueues a packet to be transmitted to the remote machine.
     */
    fun send(packet: ArtemisPacket)

    /**
     * Enqueues a Collection of packets to be transmitted to the remote machine.
     */
    fun send(packets: Collection<ArtemisPacket>)

    /**
     * Requests that the interface finish what it is doing and close the
     * connection to the remote machine.
     */
    fun stop()

    /**
     * Dispatches the given ConnectionEvent or ParseResult to listeners.
     */
    fun dispatch(event: DispatchEvent)

    /**
     * Returns whether the interface is in debug mode.
     */
    val debugMode: Boolean

    companion object {
        val MIN_VERSION = Version(2, 3, 0)
        val LATEST_VERSION = Version(2, 8, 1)
    }
}
