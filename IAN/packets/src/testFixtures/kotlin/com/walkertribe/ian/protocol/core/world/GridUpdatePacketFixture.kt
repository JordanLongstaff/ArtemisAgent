package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Version
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeFloatLe
import kotlinx.io.writeIntLe

class GridUpdatePacketFixture(override val specName: String, isFullUpdate: Boolean) :
    PacketTestFixture.Server<GridUpdatePacket>(TestPacketTypes.SHIP_SYSTEM_SYNC) {
    data class Coordinate(val index: Int) {
        val x: Byte = (index % MAX_XY).toByte()
        val y: Byte = (index / MAX_XY % MAX_XY).toByte()
        val z: Byte = (index / MAX_XY / MAX_XY).toByte()
    }

    data class Damcon(
        val team: Byte,
        val goalX: Int,
        val currentX: Int,
        val goalY: Int,
        val currentY: Int,
        val goalZ: Int,
        val currentZ: Int,
        val progress: Float,
        val members: Int,
    )

    data class Data(
        val isFullUpdate: Boolean,
        val gridStatus: Map<Coordinate, Float>,
        val damconTeams: List<Damcon>,
    ) : PacketTestData.Server<GridUpdatePacket> {
        override val version: Version
            get() = Version.DEFAULT

        override fun buildPayload(): Source = buildPacket {
            writeByte(if (isFullUpdate) 1 else 0)

            gridStatus.forEach { (coord, damage) ->
                writeByte(coord.x)
                writeByte(coord.y)
                writeByte(coord.z)
                writeFloatLe(damage)
            }
            writeByte(END_DAMAGE)

            damconTeams.forEach { damcon ->
                writeByte(damcon.team)
                writeIntLe(damcon.goalX)
                writeIntLe(damcon.currentX)
                writeIntLe(damcon.goalY)
                writeIntLe(damcon.currentY)
                writeIntLe(damcon.goalZ)
                writeIntLe(damcon.currentZ)
                writeFloatLe(damcon.progress)
                writeIntLe(damcon.members)
            }
            writeByte(END_DAMCON)
        }

        override fun validate(packet: GridUpdatePacket) {
            packet.isFullUpdate shouldBeEqual isFullUpdate
            packet.damages shouldHaveSize gridStatus.size
        }
    }

    override val generator: Gen<Data> =
        Arb.bind(Arb.map(ARB_COORD, ARB_FLOAT), Arb.list(Arb.bind<Damcon>())) {
            gridStatus,
            damconTeams ->
            Data(isFullUpdate, gridStatus, damconTeams)
        }

    override suspend fun testType(packet: Packet.Server): GridUpdatePacket =
        packet.shouldBeInstanceOf()

    companion object {
        private const val END_DAMAGE: Byte = -1
        private const val END_DAMCON: Byte = -2

        private const val MAX_XY: Byte = 5
        private const val MAX_Z: Byte = 10
        private const val MAX_COORD = MAX_XY * MAX_XY * MAX_Z

        private val ARB_COORD = Arb.int(0 until MAX_COORD).map { Coordinate(it) }
        private val ARB_FLOAT = Arb.float(0f..1f)

        val ALL =
            listOf(
                GridUpdatePacketFixture("Full update", true),
                GridUpdatePacketFixture("Partial update", false),
            )
    }
}
