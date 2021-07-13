package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.SimpleEventPacket
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.Artemis
import com.walkertribe.ian.world.EPSILON
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeNaN
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.floats.shouldNotBeNaN
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.PropertyContext
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFloatLittleEndian
import io.ktor.utils.io.core.writeIntLittleEndian

class AllShipSettingsPacketTest : PacketTestSpec.Server<AllShipSettingsPacket>(
    specName = "AllShipSettingsPacket",
    packetType = TestPacketTypes.SIMPLE_EVENT,
    packetTypeName = CorePacketType.SIMPLE_EVENT,
) {
    private lateinit var ships: List<Pair<Int, Ship>>
    private var shouldWriteAccentColor: Boolean = true

    override var version: Version = super.version

    override val protocol: PacketTestProtocol<AllShipSettingsPacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> get() = Arb.bind(
        if (shouldWriteAccentColor) Arb.int(min = 4) else Arb.int(3..3),
        Arb.nonNegativeInt(),
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
                    Ship(name.takeIf { hasName != 0 }, shipType, accentColor, drive)
                )
            },
            Artemis.SHIP_COUNT..Artemis.SHIP_COUNT,
        ),
    ) { major, patch, shipList ->
        ships = shipList
        version = Version(2, major, patch)

        buildPacket {
            writeIntLittleEndian(SimpleEventPacket.Subtype.SHIP_SETTINGS.toInt())
            ships.forEach { (hasName, ship) ->
                writeIntLittleEndian(ship.drive.ordinal)
                writeIntLittleEndian(ship.shipType)
                if (shouldWriteAccentColor) {
                    writeFloatLittleEndian(ship.accentColor)
                }
                writeIntLittleEndian(hasName)
                if (hasName != 0) {
                    writeString(ship.name.shouldNotBeNull().toString())
                }
            }
        }
    }

    override suspend fun testType(packet: ArtemisPacket): AllShipSettingsPacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: AllShipSettingsPacket) {
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

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describeTests: suspend DescribeSpecContainerScope.() -> Unit,
    ) {
        arrayOf(
            Pair("Before version 2.6.0", false),
            Pair("Since version 2.6.0", true),
        ).forEach { (name, shouldWriteColor) ->
            describe(name) {
                shouldWriteAccentColor = shouldWriteColor

                describeTests()
            }
        }
    }

    override fun collect(context: PropertyContext) {
        ships.forEach {
            context.collect(if (it.first == 0) "Does not have name" else "Has name")
        }
    }
}
