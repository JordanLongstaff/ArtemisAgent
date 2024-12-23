package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeIntLe

class DeleteObjectPacketFixture private constructor(
    arbVersion: Arb<Version>,
    targetType: ObjectType,
) : PacketTestFixture.Server<DeleteObjectPacket>(TestPacketTypes.OBJECT_DELETE) {
    class Data internal constructor(
        override val version: Version,
        private val targetType: ObjectType,
        private val targetID: Int,
    ) : PacketTestData.Server<DeleteObjectPacket> {
        override fun buildPayload(): Source = buildPacket {
            writeByte(targetType.id)
            writeIntLe(targetID)
        }

        override fun validate(packet: DeleteObjectPacket) {
            packet.targetType shouldBeEqual targetType
            packet.target shouldBeEqual targetID
        }
    }

    override val specName: String = targetType.name

    override val generator: Gen<Data> = Arb.bind(arbVersion, Arb.int()) { version, targetID ->
        Data(version, targetType, targetID)
    }

    override suspend fun testType(packet: Packet.Server): DeleteObjectPacket =
        packet.shouldBeInstanceOf()

    companion object {
        fun allFixtures(arbVersion: Arb<Version> = Arb.version()): List<DeleteObjectPacketFixture> =
            ObjectType.entries.map { DeleteObjectPacketFixture(arbVersion, it) }
    }
}
