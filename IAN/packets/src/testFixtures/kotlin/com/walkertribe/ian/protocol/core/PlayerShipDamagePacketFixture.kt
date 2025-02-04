package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import com.walkertribe.ian.world.EPSILON
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.numericFloat
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeFloatLe
import kotlinx.io.writeIntLe

class PlayerShipDamagePacketFixture(arbVersion: Arb<Version> = Arb.version()) :
    PacketTestFixture.Server<PlayerShipDamagePacket>(TestPacketTypes.SIMPLE_EVENT) {
    class Data
    internal constructor(
        override val version: Version,
        private val shipIndex: Int,
        private val damageDuration: Float,
    ) : PacketTestData.Server<PlayerShipDamagePacket> {
        override fun buildPayload(): Source = buildPacket {
            writeIntLe(SimpleEventPacket.Subtype.PLAYER_SHIP_DAMAGE.toInt())
            writeIntLe(shipIndex)
            writeFloatLe(damageDuration)
        }

        override fun validate(packet: PlayerShipDamagePacket) {
            packet.shipIndex shouldBeEqual shipIndex
            packet.duration.shouldBeWithinPercentageOf(damageDuration, EPSILON)
        }
    }

    override val generator: Gen<Data> = Arb.bind(arbVersion, Arb.int(), Arb.numericFloat(), ::Data)

    override suspend fun testType(packet: Packet.Server): PlayerShipDamagePacket =
        packet.shouldBeInstanceOf()
}
