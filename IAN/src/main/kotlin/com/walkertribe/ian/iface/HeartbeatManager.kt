package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.core.GameOverReasonPacket
import com.walkertribe.ian.protocol.core.GameStartPacket
import com.walkertribe.ian.protocol.core.HeartbeatPacket
import kotlinx.datetime.Clock

/**
 * Class responsible for tracking and sending HeartbeatPackets.
 *
 * @author rjwut
 */
class HeartbeatManager(private val iface: ArtemisNetworkInterface) {
    private var lastHeartbeatReceivedTime: Long = Clock.System.now().toEpochMilliseconds()
    private var lastHeartbeatSentTime: Long = -1
    private var isLost = false
    private var isAutoSendHeartbeat = true
    private var heartbeatTimeout: Long = DEFAULT_HEARTBEAT_TIMEOUT
    private var isActive = false

    /** Sets whether the [HeartbeatManager] should automatically send [HeartbeatPacket]s or not. */
    fun setAutoSendHeartbeat(autoSendHeartbeat: Boolean) {
        isAutoSendHeartbeat = autoSendHeartbeat
    }

    /** Sets the timeout value for listening for HeartbeatPackets. */
    fun setTimeout(timeout: Long) {
        heartbeatTimeout = timeout
    }

    /** Invoked when a [GameStartPacket] is received from the remote machine. */
    @Listener
    fun onGameStart(packet: GameStartPacket) {
        isActive = true
        resetHeartbeatTimestamp(packet.timestamp)
    }

    /** Invoked when a [GameOverReasonPacket] is received from the remote machine. */
    @Listener
    fun onGameOver(@Suppress("UNUSED_PARAMETER") packet: GameOverReasonPacket) {
        isActive = false
    }

    /** Invoked when a [HeartbeatPacket.Server] is received from the remote machine. */
    @Listener
    fun onHeartbeat(packet: HeartbeatPacket.Server) {
        resetHeartbeatTimestamp(packet.timestamp)
    }

    private fun resetHeartbeatTimestamp(timestamp: Long) {
        lastHeartbeatReceivedTime = timestamp
        if (isLost) {
            isLost = false
            iface.sendConnectionEvent(ConnectionEvent.HeartbeatRegained)
        }
    }

    /**
     * Checks to see if we need to send a [ConnectionEvent.HeartbeatLost] event, and sends it if
     * needed.
     */
    fun checkForHeartbeat() {
        if (!isActive || isLost) {
            return
        }
        val elapsed = Clock.System.now().toEpochMilliseconds() - lastHeartbeatReceivedTime
        if (elapsed >= heartbeatTimeout) {
            isLost = true
            iface.sendConnectionEvent(ConnectionEvent.HeartbeatLost)
        }
    }

    /**
     * Determines whether enough time has elapsed that we need to send a HeartbeatPacket, and sends
     * it if needed. Does nothing if autoSendHeartbeat is set to false.
     */
    fun sendHeartbeatIfNeeded() {
        if (!isAutoSendHeartbeat) {
            return
        }
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastHeartbeatSentTime >= HEARTBEAT_SEND_INTERVAL_MS) {
            iface.sendPacket(HeartbeatPacket.Client)
            lastHeartbeatSentTime = now
        }
    }

    companion object {
        private const val HEARTBEAT_SEND_INTERVAL_MS: Long = 3000
        private const val DEFAULT_HEARTBEAT_TIMEOUT: Long = 15000
    }
}
