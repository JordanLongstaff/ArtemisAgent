package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.core.GameStartPacket
import com.walkertribe.ian.protocol.core.HeartbeatPacket

/**
 * Class responsible for tracking and sending HeartbeatPackets.
 * @author rjwut
 */
class HeartbeatManager(private val iface: ArtemisNetworkInterface) {
    private val startTime = System.currentTimeMillis()
    private var lastHeartbeatReceivedTime: Long = -1
    private var lastHeartbeatSentTime: Long = -1
    private var isLost = false
    private var isAutoSendHeartbeat = true
    private var heartbeatTimeout: Long = DEFAULT_HEARTBEAT_TIMEOUT
    private var isActive = false

    /**
     * Sets whether the HeartbeatManager should send HeartbeatPackets or not.
     */
    fun setAutoSendHeartbeat(autoSendHeartbeat: Boolean) {
        isAutoSendHeartbeat = autoSendHeartbeat
    }

    /**
     * Sets the timeout value for listening for HeartbeatPackets.
     */
    fun setTimeout(timeout: Long) {
        heartbeatTimeout = timeout
    }

    /**
     * Invoked when a GameStartPacket is received from the remote machine.
     */
    fun onGameStart(packet: GameStartPacket) {
        isActive = true
        resetHeartbeatTimestamp(packet.timestamp)
    }

    /**
     * Invoked when a GameOverReasonPacket is received from the remote machine.
     */
    fun onGameOver() {
        isActive = false
    }

    /**
     * Invoked when a HeartbeatPacket is received from the remote machine.
     */
    fun onHeartbeat(packet: HeartbeatPacket) {
        resetHeartbeatTimestamp(packet.timestamp)
    }

    private fun resetHeartbeatTimestamp(timestamp: Long) {
        lastHeartbeatReceivedTime = timestamp
        if (isLost) {
            isLost = false
            iface.dispatch(ConnectionEvent.HeartbeatRegained())
        }
    }

    /**
     * Checks to see if we need to send a HeartbeatLostEvent, and sends it if needed.
     */
    fun checkForHeartbeat() {
        if (!isActive || isLost) {
            return
        }
        val fromTime =
            if (lastHeartbeatReceivedTime == -1L) startTime else lastHeartbeatReceivedTime
        val elapsed = System.currentTimeMillis() - fromTime
        if (elapsed >= heartbeatTimeout) {
            isLost = true
            iface.dispatch(ConnectionEvent.HeartbeatLost())
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
        val now = System.currentTimeMillis()
        if (now - lastHeartbeatSentTime >= HEARTBEAT_SEND_INTERVAL_MS) {
            iface.send(HeartbeatPacket.Client)
            lastHeartbeatSentTime = now
        }
    }

    companion object {
        private const val HEARTBEAT_SEND_INTERVAL_MS: Long = 3000
        private const val DEFAULT_HEARTBEAT_TIMEOUT: Long = 15000
    }
}
