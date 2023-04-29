package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.Version

/** Interface for objects which can connect to an Artemis server and send and receive packets. */
interface ArtemisNetworkInterface {
    /** Returns the version of Artemis used by this interface. */
    val version: Version

    /**
     * Returns the max version of Artemis supported by this interface. If null, there is no upper
     * bound.
     */
    val maxVersion: Version?

    fun addListenerModule(module: ListenerModule)

    fun addListeners(listeners: Iterable<ListenerFunction<out ListenerArgument>>) {
        addListenerModule(CompositeListenerModule(listeners.toList()))
    }

    /**
     * Sets whether heartbeat packets should be sent to the remote machine automatically. Defaults
     * to true. Set this to false if you pass this object to another interface's proxyTo() method
     * and don't capture heartbeat packets in any of your listeners.
     */
    fun setAutoSendHeartbeat(autoSendHeartbeat: Boolean)

    /** Sets the timeout value for listening for heartbeat packets. */
    fun setTimeout(timeout: Long)

    /**
     * Attempts an outgoing client connection to an Artemis server. The send and receive streams
     * won't actually be opened until [start] is called.
     *
     * @param host the hostname of the server.
     * @param port the port on which to connect.
     * @param timeoutMs how long (in milliseconds) IAN will wait for the connection to be
     *   established before returning false.
     * @return Whether the connection was successful.
     */
    suspend fun connect(host: String, port: Int, timeoutMs: Long): Boolean

    /** Opens the send/receive streams to the remote machine. */
    fun start()

    /** Returns true if currently connected to the remote machine; false otherwise. */
    val isConnected: Boolean

    /** Enqueues a packet to be transmitted to the remote machine. */
    fun sendPacket(packet: Packet.Client)

    /**
     * Requests that the interface finish what it is doing and close the connection to the remote
     * machine.
     */
    fun stop()

    /** Dispatches the given [ConnectionEvent] to listeners. */
    fun sendConnectionEvent(event: ConnectionEvent)

    /** Disposes of all resources used by the interface. */
    fun dispose()
}
