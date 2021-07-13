package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class DeleteObjectPacketTest : PacketTestSpec.Server<DeleteObjectPacket>(
    specName = "DeleteObjectPacket",
    packetType = TestPacketTypes.OBJECT_DELETE,
    packetTypeName = CorePacketType.OBJECT_DELETE,
) {
    private val allObjectTypes = ObjectType.entries

    private lateinit var targetType: ObjectType
    private var targetID: Int = 0

    override val protocol: PacketTestProtocol<DeleteObjectPacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> get() = Arb.int().map {
        targetID = it

        buildPacket {
            writeByte(targetType.id)
            writeIntLittleEndian(targetID)
        }
    }

    override val failures: List<Failure> = listOf(
        object : Failure("Fails to parse invalid object types") {
            private val validObjectTypeIDs = allObjectTypes.map { it.id }.toSet()

            override val payloadGen: Gen<ByteReadPacket> = Arb.bind(
                Arb.byte().filterNot(validObjectTypeIDs::contains),
                Arb.int(),
            ) { type, id ->
                buildPacket {
                    writeByte(type)
                    writeIntLittleEndian(id)
                }
            }
        }
    )

    override suspend fun testType(packet: ArtemisPacket): DeleteObjectPacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: DeleteObjectPacket) {
        packet.targetType shouldBeEqual targetType
        packet.target shouldBeEqual targetID
    }

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describeTests: suspend DescribeSpecContainerScope.() -> Unit,
    ) {
        allObjectTypes.forEach {
            describe(it.name) {
                targetType = it

                describeTests()
            }
        }
    }

    override suspend fun describeClientTests(
        scope: DescribeSpecContainerScope,
        client: ArtemisNetworkInterface,
        sendChannel: ByteWriteChannel,
        afterTest: () -> Unit
    ) {
        scope.describe(specName) {
            withData(
                nameFn = { "Object type: ${it.name}" },
                allObjectTypes,
            ) {
                targetType = it
                runClientTest(client, sendChannel)
                afterTest()
            }
        }
    }
}
