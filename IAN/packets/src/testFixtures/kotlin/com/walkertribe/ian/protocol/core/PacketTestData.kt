package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.Version
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.longs.shouldBeZero
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readIntLittleEndian

sealed interface PacketTestData<T : Packet> {
    abstract class Client<T : Packet.Client>(val packet: T) : PacketTestData<T> {
        abstract fun validatePayload(payload: ByteReadPacket)

        fun validate(payload: ByteReadPacket, expectedSize: Int, expectedPacketType: Int) {
            payload.remaining.toInt() shouldBeEqual expectedSize + Int.SIZE_BYTES
            payload.readIntLittleEndian() shouldBeEqual expectedPacketType
            validatePayload(payload)
            payload.remaining.shouldBeZero()
            payload.close()
        }
    }

    interface Server<T : Packet.Server> : PacketTestData<T> {
        val version: Version

        fun buildPayload(): ByteReadPacket

        fun validate(packet: T)
    }
}
