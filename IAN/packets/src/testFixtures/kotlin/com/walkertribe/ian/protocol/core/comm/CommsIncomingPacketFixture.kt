package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Util.caretToNewline
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.short
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeIntLe
import kotlinx.io.writeShortLe

class CommsIncomingPacketFixture
private constructor(
    override val specName: String,
    isUsingCommFilters: Boolean,
    versionArb: Arb<Version>,
) : PacketTestFixture.Server<CommsIncomingPacket>(TestPacketTypes.COMM_TEXT) {
    data class Data(
        override val version: Version,
        val sender: String,
        val message: String,
        val isUsingCommFilters: Boolean,
        val channel: Int,
    ) : PacketTestData.Server<CommsIncomingPacket> {
        override fun buildPayload(): Source = buildPacket {
            if (isUsingCommFilters) {
                writeShortLe(channel.toShort())
            } else {
                writeIntLe(channel)
            }
            writeString(sender)
            writeString(message)
        }

        override fun validate(packet: CommsIncomingPacket) {
            packet.sender shouldBeEqual sender
            packet.message shouldBeEqual message.caretToNewline().trim()
        }
    }

    override val generator: Gen<Data> =
        Arb.bind(
            versionArb,
            Arb.string(),
            Arb.string(),
            if (isUsingCommFilters) {
                Arb.short().map(Short::toInt)
            } else {
                Arb.int()
            },
        ) { version, from, contents, channelValue ->
            Data(version, from, contents, isUsingCommFilters, channelValue)
        }

    override suspend fun testType(packet: Packet.Server): CommsIncomingPacket =
        packet.shouldBeInstanceOf()

    companion object {
        val ALL =
            listOf(
                CommsIncomingPacketFixture("Before version 2.6.0", false, Arb.version(2, 3..5)),
                CommsIncomingPacketFixture(
                    "Since version 2.6.0",
                    true,
                    Arb.version(2, Arb.int(min = 6)),
                ),
            )
    }
}
