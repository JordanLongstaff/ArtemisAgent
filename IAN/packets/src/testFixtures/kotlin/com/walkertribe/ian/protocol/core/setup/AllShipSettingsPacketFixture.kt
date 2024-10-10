package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.SimpleEventPacket
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import com.walkertribe.ian.world.Artemis
import com.walkertribe.ian.world.EPSILON
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeNaN
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.floats.shouldNotBeNaN
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeFloatLe
import kotlinx.io.writeIntLe

class AllShipSettingsPacketFixture private constructor(
    override val specName: String,
    private val shouldWriteAccentColor: Boolean,
    versionArb: Arb<Version>,
) : PacketTestFixture.Server<AllShipSettingsPacket>(TestPacketTypes.SIMPLE_EVENT) {
    data class Data(
        override val version: Version,
        val shouldWriteAccentColor: Boolean,
        val ships: List<Pair<Int, Ship>>,
    ) : PacketTestData.Server<AllShipSettingsPacket> {
        override fun buildPayload(): Source = buildPacket {
            writeIntLe(SimpleEventPacket.Subtype.SHIP_SETTINGS.toInt())
            ships.forEach { (hasName, ship) ->
                writeIntLe(ship.drive.ordinal)
                writeIntLe(ship.shipType)
                if (shouldWriteAccentColor) {
                    writeFloatLe(ship.accentColor)
                }
                writeIntLe(hasName)
                if (hasName != 0) {
                    writeString(ship.name.shouldNotBeNull())
                }
            }
        }

        override fun validate(packet: AllShipSettingsPacket) {
            packet.ships shouldContainExactly ships.map { it.second }
            packet.ships.indices.forEach { index ->
                val ship = packet[index]
                val counterpart = ships[index].second
                ship shouldBeEqual counterpart

                (ship.name == null) shouldBeEqual (ships[index].first == 0)
                if (shouldWriteAccentColor) {
                    ship.hue.shouldNotBeNaN()
                    ship.hue.shouldBeWithinPercentageOf(counterpart.hue, EPSILON)
                } else {
                    ship.hue.shouldBeNaN()
                }
            }
        }
    }

    override val generator: Gen<Data> = Arb.bind(
        versionArb,
        Arb.list(
            Arb.bind(
                Arb.int(),
                Arb.string(),
                Arb.int(),
                if (shouldWriteAccentColor) {
                    Arb.numericFloat(min = 0f, max = 1f)
                } else {
                    Arb.of(Float.NaN)
                },
                Arb.enum<DriveType>(),
            ) { hasName, name, shipType, accentColor, drive ->
                Pair(
                    hasName,
                    Ship(name.takeIf { hasName != 0 }, shipType, drive, accentColor)
                )
            },
            Artemis.SHIP_COUNT..Artemis.SHIP_COUNT,
        ),
    ) { version, ships -> Data(version, shouldWriteAccentColor, ships) }

    override suspend fun testType(packet: Packet.Server): AllShipSettingsPacket =
        packet.shouldBeInstanceOf()

    companion object {
        val ALL = listOf(
            AllShipSettingsPacketFixture(
                "Before version 2.4.0",
                false,
                Arb.version(2, 3),
            ),
            AllShipSettingsPacketFixture(
                "Since version 2.4.0",
                true,
                Arb.version(2, Arb.int(min = 4)),
            ),
        )
    }
}
