package com.walkertribe.ian.protocol.core

/**
 * Defines the known packet types for the core Artemis protocol.
 * @author rjwut
 */
object CorePacketType {
    const val CARRIER_RECORD = "carrierRecord"
    const val COMMS_BUTTON = "commsButton"
    const val COMMS_MESSAGE = "commsMessage"
    const val COMM_TEXT = "commText"
    const val CONNECTED = "connected"
    const val CONTROL_MESSAGE = "controlMessage"
    const val HEARTBEAT = "heartbeat"
    const val INCOMING_MESSAGE = "incomingMessage"
    const val OBJECT_BIT_STREAM = "objectBitStream"
    const val OBJECT_DELETE = "objectDelete"
    const val OBJECT_TEXT = "objectText"
    const val PLAIN_TEXT_GREETING = "plainTextGreeting"
    const val SIMPLE_EVENT = "simpleEvent"
    const val START_GAME = "startGame"
    const val VALUE_INT = "valueInt"
}
