package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.iface.ListenerRegistry
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.ParseResult
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Version
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.checkAll
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFloatLittleEndian
import io.ktor.utils.io.core.writeIntLittleEndian
import kotlin.time.Duration.Companion.seconds

class VersionPacketTest : PacketTestSpec.Server<VersionPacket>(
    specName = "VersionPacket",
    packetType = TestPacketTypes.CONNECTED,
    packetTypeName = CorePacketType.CONNECTED,
    needsListeners = false,
) {
    private var unknownInt: Int = 0
    private var legacyFloat: Float = 0f
    var majorVersion: Int = 0
    var minorVersion: Int = 0
    var patchVersion: Int = 0

    override val protocol: PacketTestProtocol<VersionPacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> = Arb.bind(
        Arb.int(),
        Arb.float(),
        Arb.nonNegativeInt(),
        Arb.nonNegativeInt(),
        Arb.nonNegativeInt(),
    ) { unknown, legacy, major, minor, patch ->
        unknownInt = unknown
        legacyFloat = legacy
        majorVersion = major
        minorVersion = minor
        patchVersion = patch

        buildPacket()
    }

    override val failures: List<Failure> = listOf(
        object : Failure("Fails to parse legacy version") {
            override val payloadGen: Gen<ByteReadPacket> = Arb.bind(
                Arb.int(),
                Arb.float(),
            ) { unknown, legacy ->
                buildPacket {
                    writeIntLittleEndian(unknown)
                    writeFloatLittleEndian(legacy)
                }
            }
        }
    )

    override suspend fun testType(packet: ArtemisPacket): VersionPacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: VersionPacket) {
        packet.version shouldBeEqual Version(majorVersion, minorVersion, patchVersion)
    }

    override suspend fun generateTest(
        client: ArtemisNetworkInterface,
        runTest: suspend (ByteReadPacket) -> Unit,
    ) {
        checkAll(
            Arb.int(),
            Arb.float(),
            Arb.choose(
                2 to Arb.pair(Arb.of(8), Arb.of(0, 1)),
                998 to Arb.pair(Arb.int(3..7), Arb.nonNegativeInt()),
            ),
        ) { unknown, legacy, (minor, patch) ->
            unknownInt = unknown
            legacyFloat = legacy
            majorVersion = 2
            minorVersion = minor
            patchVersion = patch

            runTest(buildPacket())
            eventually(SERVER_TIMEOUT.seconds) {
                client.version shouldBeEqual Version(majorVersion, minorVersion, patchVersion)
            }
        }
    }

    override suspend fun DescribeSpecContainerScope.describeMore(
        readChannel: ByteReadChannel,
    ) {
        it("Sets version of PacketReader") {
            val reader = PacketReader(
                readChannel,
                protocol,
                ListenerRegistry(),
            )

            payloadGen.checkAll { payload ->
                readChannel.prepareMockPacket(payload, packetType)

                val result = reader.readPacket()
                result.shouldBeInstanceOf<ParseResult.Success>()

                val packet = result.packet
                packet.shouldBeInstanceOf<VersionPacket>()
                reader.version shouldBeEqual packet.version

                reader.close()
            }
        }
    }

    fun buildPacket(): ByteReadPacket = buildPacket {
        writeIntLittleEndian(unknownInt)
        writeFloatLittleEndian(legacyFloat)
        writeIntLittleEndian(majorVersion)
        writeIntLittleEndian(minorVersion)
        writeIntLittleEndian(patchVersion)
    }
}
