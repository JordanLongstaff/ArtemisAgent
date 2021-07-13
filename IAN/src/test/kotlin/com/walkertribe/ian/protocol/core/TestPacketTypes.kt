package com.walkertribe.ian.protocol.core

object TestPacketTypes {
    const val CARRIER_RECORD = 0x9ad1f23b.toInt()
    const val COMMS_BUTTON = 0xca88f050.toInt()
    const val COMM_TEXT = 0xd672c35f.toInt()
    const val CONNECTED = 0xe548e74a.toInt()
    const val HEARTBEAT = 0xf5821226.toInt()
    const val INCOMING_MESSAGE = 0xae88e058.toInt()
    const val OBJECT_BIT_STREAM = 0x80803df9.toInt()
    const val OBJECT_DELETE = 0xcc5a3e30.toInt()
    const val OBJECT_TEXT = 0xee665279.toInt()
    const val PLAIN_TEXT_GREETING = 0x6d04b3da
    const val SIMPLE_EVENT = 0xf754c8fe.toInt()
    const val START_GAME = 0x3de66711

    const val COMMS_MESSAGE = 0x574c4c4b
    const val CONTROL_MESSAGE = 0x6aadc57f
    const val VALUE_INT = 0x4c821d3c
}
