package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.world.EPSILON
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.numericFloat
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFloatLittleEndian
import io.ktor.utils.io.core.writeIntLittleEndian

class PlayerShipDamagePacketTest : PacketTestSpec.Server<PlayerShipDamagePacket>(
    specName = "PlayerShipDamagePacket",
    packetType = TestPacketTypes.SIMPLE_EVENT,
    packetTypeName = CorePacketType.SIMPLE_EVENT,
) {
    private var shipIndex: Int = 0
    private var damageDuration: Float = 0f

    override val protocol: PacketTestProtocol<PlayerShipDamagePacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> = Arb.bind(
        Arb.int(),
        Arb.numericFloat(),
    ) { index, duration ->
        shipIndex = index
        damageDuration = duration

        buildPacket {
            writeIntLittleEndian(SimpleEventPacket.Subtype.PLAYER_SHIP_DAMAGE.toInt())
            writeIntLittleEndian(shipIndex)
            writeFloatLittleEndian(damageDuration)
        }
    }

    override suspend fun testType(packet: ArtemisPacket): PlayerShipDamagePacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: PlayerShipDamagePacket) {
        packet.shipIndex shouldBeEqual shipIndex
        packet.duration.shouldBeWithinPercentageOf(damageDuration, EPSILON)
    }
}
