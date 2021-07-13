package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Util.caretToNewline
import com.walkertribe.ian.util.Version
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian
import io.ktor.utils.io.core.writeShortLittleEndian

class CommsIncomingPacketTest : PacketTestSpec.Server<CommsIncomingPacket>(
    specName = "CommsIncomingPacket",
    packetType = TestPacketTypes.COMM_TEXT,
    packetTypeName = CorePacketType.COMM_TEXT,
) {
    private var commFilters: Boolean = true
    private var sender: String = ""
    private var message: String = ""
    private var channel: Int = 0

    override var version: Version = super.version

    override val protocol: PacketTestProtocol<CommsIncomingPacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> get() {
        val (majorGen, channelGen) = if (commFilters) {
            Pair(Arb.int(min = 6), Arb.int(Short.MIN_VALUE..Short.MAX_VALUE))
        } else {
            Pair(Arb.int(3..5), Arb.int())
        }

        return Arb.bind(
            majorGen,
            Arb.nonNegativeInt(),
            Arb.string(),
            Arb.string(),
            channelGen,
        ) { major, patch, from, contents, channelValue ->
            version = Version(2, major, patch)
            sender = from
            message = contents
            channel = channelValue

            buildPacket {
                if (commFilters) {
                    writeShortLittleEndian(channel.toShort())
                } else {
                    writeIntLittleEndian(channel)
                }
                writeString(sender)
                writeString(message)
            }
        }
    }

    override suspend fun testType(packet: ArtemisPacket): CommsIncomingPacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: CommsIncomingPacket) {
        packet.sender.toString() shouldBeEqual sender
        packet.message.toString() shouldBeEqual message.caretToNewline().trim()
    }

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describeTests: suspend DescribeSpecContainerScope.() -> Unit,
    ) {
        arrayOf(
            Pair("Before version 2.6.0", false),
            Pair("Since version 2.6.0", true),
        ).forEach { (name, filters) ->
            describe(name) {
                commFilters = filters

                describeTests()
            }
        }
    }
}
