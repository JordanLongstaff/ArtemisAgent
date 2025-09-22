package com.walkertribe.ian.protocol.core.comm

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
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source

class CommsButtonPacketFixture
private constructor(override val specName: String, override val generator: Gen<Data>) :
    PacketTestFixture.Server<CommsButtonPacket>(TestPacketTypes.COMMS_BUTTON) {
    sealed class Data(
        override val version: Version,
        private val actionValue: Byte,
        open val label: String?,
    ) : PacketTestData.Server<CommsButtonPacket> {
        class Remove(version: Version, override val label: String) : Data(version, REMOVE, label) {
            override fun validate(packet: CommsButtonPacket) {
                packet.action
                    .shouldBeInstanceOf<CommsButtonPacket.Action.Remove>()
                    .label shouldBeEqual label
            }
        }

        class Create(version: Version, override val label: String) : Data(version, CREATE, label) {
            override fun validate(packet: CommsButtonPacket) {
                packet.action
                    .shouldBeInstanceOf<CommsButtonPacket.Action.Create>()
                    .label shouldBeEqual label
            }
        }

        class RemoveAll(version: Version) : Data(version, REMOVE_ALL, null) {
            override fun validate(packet: CommsButtonPacket) {
                packet.action.shouldBeInstanceOf<CommsButtonPacket.Action.RemoveAll>()
            }
        }

        final override fun buildPayload(): Source = buildPacket {
            writeByte(actionValue)
            label?.also { writeString(it) }
        }
    }

    override suspend fun testType(packet: Packet.Server): CommsButtonPacket =
        packet.shouldBeInstanceOf()

    companion object {
        private const val REMOVE: Byte = 0x00
        private const val CREATE: Byte = 0x02
        private const val REMOVE_ALL: Byte = 0x64

        val ALL_VALID_ACTIONS = setOf(REMOVE, CREATE, REMOVE_ALL)

        fun allFixtures(
            arbVersion: Arb<Version> = Arb.version(),
            arbLabels: Arb<String> = Arb.string(),
        ): List<CommsButtonPacketFixture> =
            listOf(
                CommsButtonPacketFixture("Remove", Arb.bind(arbVersion, arbLabels, Data::Remove)),
                CommsButtonPacketFixture("Create", Arb.bind(arbVersion, arbLabels, Data::Create)),
                CommsButtonPacketFixture("Remove All", arbVersion.map(Data::RemoveAll)),
            )
    }
}
