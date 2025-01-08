package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.map
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeIntLe

class PausePacketFixture private constructor(arbVersion: Arb<Version>, isPaused: BoolState) :
    PacketTestFixture.Server<PausePacket>(TestPacketTypes.SIMPLE_EVENT) {
    override val specName: String = "Paused: $isPaused"

    class Data
    internal constructor(override val version: Version, private val isPaused: BoolState) :
        PacketTestData.Server<PausePacket> {
        override fun buildPayload(): Source = buildPacket {
            writeIntLe(SimpleEventPacket.Subtype.PAUSE.toInt())
            writeIntLe(if (isPaused.booleanValue) 1 else 0)
        }

        override fun validate(packet: PausePacket) {
            packet.isPaused shouldBeEqual isPaused
        }
    }

    override val generator: Gen<Data> = arbVersion.map { Data(it, isPaused) }

    override suspend fun testType(packet: Packet.Server): PausePacket = packet.shouldBeInstanceOf()

    companion object {
        fun allFixtures(arbVersion: Arb<Version> = Arb.version()): List<PausePacketFixture> =
            listOf(BoolState.True, BoolState.False).map { PausePacketFixture(arbVersion, it) }
    }
}
