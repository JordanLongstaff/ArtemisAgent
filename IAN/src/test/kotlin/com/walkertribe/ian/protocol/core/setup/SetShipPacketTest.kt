package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.protocol.core.ValueIntPacket
import com.walkertribe.ian.world.Artemis
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.negativeInt
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readIntLittleEndian

class SetShipPacketTest : PacketTestSpec.Client<SetShipPacket>(
    specName = "SetShipPacket",
    packetType = TestPacketTypes.VALUE_INT,
    packetTypeName = CorePacketType.VALUE_INT,
    expectedPayloadSize = Int.SIZE_BYTES * 2,
) {
    private var clientShipIndex: Int = 0

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describe: suspend DescribeSpecContainerScope.(Gen<SetShipPacket>) -> Unit,
    ) {
        repeat(Artemis.SHIP_COUNT) {
            describe("Ship index: $it") {
                describe(Exhaustive.of(SetShipPacket(it)))
            }
        }
    }

    override suspend fun runTest(packet: SetShipPacket, payload: ByteReadPacket) {
        payload.readIntLittleEndian() shouldBeEqual ValueIntPacket.Subtype.SET_SHIP.toInt()
        payload.readIntLittleEndian() shouldBeEqual packet.shipIndex
    }

    override suspend fun runClientTest(
        scope: ContainerScope,
        client: ArtemisNetworkInterface,
        readChannel: ByteReadChannel,
    ) {
        scope.withData(nameFn = { "Ship index: $it" }, 0 until Artemis.SHIP_COUNT) {
            clientShipIndex = it
            super.runClientTest(this, client, readChannel)
        }
    }

    override fun clientPacketGen(client: ArtemisNetworkInterface): Gen<SetShipPacket> {
        return Exhaustive.of(SetShipPacket(clientShipIndex))
    }

    override suspend fun DescribeSpecContainerScope.describeMore() {
        describe("Invalid ship index throws") {
            it("Negative") {
                Arb.negativeInt().checkAll {
                    shouldThrow<IllegalArgumentException> { SetShipPacket(it) }
                }
            }

            it("Too high") {
                Arb.int(min = Artemis.SHIP_COUNT).checkAll {
                    shouldThrow<IllegalArgumentException> { SetShipPacket(it) }
                }
            }
        }
    }
}
