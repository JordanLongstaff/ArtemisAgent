package com.walkertribe.ian.iface

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * An event regarding the connection to a remote machine.
 *
 * @author rjwut
 */
sealed class ConnectionEvent : ListenerArgument {
    /**
     * An event that gets thrown when IAN successfully connects to a remote machine.
     *
     * @author rjwut
     */
    data class Success(val message: String) : ConnectionEvent()

    /**
     * An event that gets thrown when an existing connection to a remote machine is lost.
     *
     * @property cause Indicates why the connection was lost.
     * @constructor
     */
    data class Disconnect(
        /**
         * Returns a [DisconnectCause] value describing the reason the connection was terminated.
         */
        val cause: DisconnectCause
    ) : ConnectionEvent()

    /**
     * An event that is raised when the ThreadedArtemisNetworkInterface has not received a heartbeat
     * packet recently.
     *
     * @author rjwut
     */
    data object HeartbeatLost : ConnectionEvent()

    /**
     * An event that is raised when the ThreadedArtemisNetworkInterface receives a heartbeat packet
     * HeartbeatLostEvent was raised.
     *
     * @author rjwut
     */
    data object HeartbeatRegained : ConnectionEvent()

    @OptIn(ExperimentalTime::class)
    final override val timestamp: Long = Clock.System.now().toEpochMilliseconds()

    final override fun offerTo(module: ListenerModule) {
        module.onConnectionEvent(this)
    }
}
