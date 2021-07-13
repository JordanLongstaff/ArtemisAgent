package com.walkertribe.ian.iface

/**
 * An event regarding the connection to a remote machine.
 * @author rjwut
 */
sealed class ConnectionEvent : DispatchEvent, ListenerArgument {
    /**
     * An event that gets thrown when IAN successfully connects to a remote machine.
     * @author rjwut
     */
    data class Success(val message: String) : ConnectionEvent()

    /**
     * An event that gets thrown when an existing connection to a remote machine is
     * lost. The {@link #cause} property indicates why the connection was lost. If
     * there is an exception that further explains the event, it is provided in the
     * {@link #exception} property; otherwise, it will be null.
     * @author rjwut
     */
    data class Disconnect(
        /**
         * Returns a DisconnectCause value describing the reason the connection was
         * terminated.
         */
        val cause: DisconnectCause
    ) : ConnectionEvent()

    /**
     * An event that is raised when the ThreadedArtemisNetworkInterface has not received a heartbeat
     * packet recently.
     * @author rjwut
     */
    class HeartbeatLost : ConnectionEvent()

    /**
     * An event that is raised when the ThreadedArtemisNetworkInterface receives a heartbeat packet
     * HeartbeatLostEvent was raised.
     * @author rjwut
     */
    class HeartbeatRegained : ConnectionEvent()

    override val timestamp: Long = System.currentTimeMillis()
}
