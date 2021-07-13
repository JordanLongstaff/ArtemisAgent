package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Version
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.bytes
import io.kotest.property.exhaustive.filterNot
import io.kotest.property.exhaustive.map
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class ObjectUpdatePacketTest : PacketTestSpec.Server<ObjectUpdatePacket>(
    specName = "ObjectUpdatePacket",
    packetType = TestPacketTypes.OBJECT_BIT_STREAM,
    packetTypeName = CorePacketType.OBJECT_BIT_STREAM,
) {
    private val allObjectTypes = ObjectType.entries

    private lateinit var parserTestConfig: ObjectParserTestConfig

    override val protocol: PacketTestProtocol<ObjectUpdatePacket> = PacketTestProtocol()
    override val version: Version get() = parserTestConfig.version

    override val payloadGen: Gen<ByteReadPacket> get() = parserTestConfig.objectPacketGen

    override val failures: List<Failure> = listOf(
        object : Failure("Fails to parse invalid object type") {
            private val validObjectTypeIDs = allObjectTypes.map { it.id }.toSet() + setOf(0)

            override val payloadGen: Gen<ByteReadPacket> = Exhaustive.bytes().filterNot {
                validObjectTypeIDs.contains(it)
            }.map {
                buildPacket {
                    writeIntLittleEndian(it.toInt())
                }
            }
        }
    )

    override suspend fun testType(packet: ArtemisPacket): ObjectUpdatePacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: ObjectUpdatePacket) {
        if (parserTestConfig.shouldYieldObject) {
            packet.objects.size shouldBeEqual 1
            packet.objects.forEach(parserTestConfig::test)
        } else {
            packet.objects.shouldBeEmpty()
        }
    }

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describeTests: suspend DescribeSpecContainerScope.() -> Unit,
    ) {
        ALL_CONFIGS.forEach { config ->
            parserTestConfig = config

            describe(config.toString()) {
                config.split(this, describeTests)
            }
        }
    }

    override suspend fun describeClientTests(
        scope: DescribeSpecContainerScope,
        client: ArtemisNetworkInterface,
        sendChannel: ByteWriteChannel,
        afterTest: () -> Unit,
    ) {
        scope.describe(specName) {
            withData(nameFn = { it.toString() }, ALL_CONFIGS) {
                parserTestConfig = it
                runClientTest(client, sendChannel)
                afterTest()
            }
        }
    }

    override suspend fun generateTest(
        client: ArtemisNetworkInterface,
        runTest: suspend (ByteReadPacket) -> Unit
    ) {
        parserTestConfig.versionArb = Arb.of(client.version)
        payloadGen.checkAll { runTest(it) }
    }

    private companion object {
        val ALL_CONFIGS = listOf(
            ObjectParserTestConfig.Empty,
            ObjectParserTestConfig.BaseParser,
            ObjectParserTestConfig.BlackHoleParser,
            ObjectParserTestConfig.CreatureParser,
            ObjectParserTestConfig.MineParser,
            ObjectParserTestConfig.NpcShipParser,
            ObjectParserTestConfig.PlayerShipParser,
            ObjectParserTestConfig.UpgradesParser,
            ObjectParserTestConfig.WeaponsParser,
            ObjectParserTestConfig.Unobserved.Anomaly,
            ObjectParserTestConfig.Unobserved.Asteroid,
            ObjectParserTestConfig.Unobserved.Drone,
            ObjectParserTestConfig.Unobserved.Engineering,
            ObjectParserTestConfig.Unobserved.GenericMesh,
            ObjectParserTestConfig.Unobserved.Nebula,
            ObjectParserTestConfig.Unobserved.Torpedo,
        )
    }
}
