package com.walkertribe.ian.protocol

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.core.ActivateUpgradePacket
import com.walkertribe.ian.protocol.core.BayStatusPacket
import com.walkertribe.ian.protocol.core.ButtonClickPacket
import com.walkertribe.ian.protocol.core.EndGamePacket
import com.walkertribe.ian.protocol.core.GameOverReasonPacket
import com.walkertribe.ian.protocol.core.GameStartPacket
import com.walkertribe.ian.protocol.core.HeartbeatPacket
import com.walkertribe.ian.protocol.core.JumpEndPacket
import com.walkertribe.ian.protocol.core.PausePacket
import com.walkertribe.ian.protocol.core.PlayerShipDamagePacket
import com.walkertribe.ian.protocol.core.SimpleEventPacket
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.protocol.core.comm.AudioCommandPacket
import com.walkertribe.ian.protocol.core.comm.CommsButtonPacket
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket
import com.walkertribe.ian.protocol.core.comm.IncomingAudioPacket
import com.walkertribe.ian.protocol.core.comm.ToggleRedAlertPacket
import com.walkertribe.ian.protocol.core.setup.AllShipSettingsPacket
import com.walkertribe.ian.protocol.core.setup.ReadyPacket
import com.walkertribe.ian.protocol.core.setup.SetConsolePacket
import com.walkertribe.ian.protocol.core.setup.SetShipPacket
import com.walkertribe.ian.protocol.core.setup.VersionPacket
import com.walkertribe.ian.protocol.core.setup.WelcomePacket
import com.walkertribe.ian.protocol.core.world.BiomechRagePacket
import com.walkertribe.ian.protocol.core.world.DeleteObjectPacket
import com.walkertribe.ian.protocol.core.world.DockedPacket
import com.walkertribe.ian.protocol.core.world.IntelPacket
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacket
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.ints

class ProtocolTest : DescribeSpec({
    describe("Protocol") {
        val allServerPacketClasses = listOf(
            Triple(
                AllShipSettingsPacket::class,
                TestPacketTypes.SIMPLE_EVENT,
                SimpleEventPacket.Subtype.SHIP_SETTINGS,
            ),
            Triple(
                BayStatusPacket::class,
                TestPacketTypes.CARRIER_RECORD,
                0.toByte(),
            ),
            Triple(
                BiomechRagePacket::class,
                TestPacketTypes.SIMPLE_EVENT,
                SimpleEventPacket.Subtype.BIOMECH_STANCE,
            ),
            Triple(
                CommsButtonPacket::class,
                TestPacketTypes.COMMS_BUTTON,
                0.toByte(),
            ),
            Triple(
                CommsIncomingPacket::class,
                TestPacketTypes.COMM_TEXT,
                0.toByte(),
            ),
            Triple(
                DeleteObjectPacket::class,
                TestPacketTypes.OBJECT_DELETE,
                0.toByte(),
            ),
            Triple(
                DockedPacket::class,
                TestPacketTypes.SIMPLE_EVENT,
                SimpleEventPacket.Subtype.DOCKED,
            ),
            Triple(
                EndGamePacket::class,
                TestPacketTypes.SIMPLE_EVENT,
                SimpleEventPacket.Subtype.END_GAME,
            ),
            Triple(
                GameOverReasonPacket::class,
                TestPacketTypes.SIMPLE_EVENT,
                SimpleEventPacket.Subtype.GAME_OVER_REASON,
            ),
            Triple(
                GameStartPacket::class,
                TestPacketTypes.START_GAME,
                0.toByte(),
            ),
            Triple(
                HeartbeatPacket.Server::class,
                TestPacketTypes.HEARTBEAT,
                0.toByte(),
            ),
            Triple(
                IncomingAudioPacket::class,
                TestPacketTypes.INCOMING_MESSAGE,
                0.toByte(),
            ),
            Triple(
                IntelPacket::class,
                TestPacketTypes.OBJECT_TEXT,
                0.toByte(),
            ),
            Triple(
                JumpEndPacket::class,
                TestPacketTypes.SIMPLE_EVENT,
                SimpleEventPacket.Subtype.JUMP_END,
            ),
            Triple(
                ObjectUpdatePacket::class,
                TestPacketTypes.OBJECT_BIT_STREAM,
                0.toByte(),
            ),
            Triple(
                PausePacket::class,
                TestPacketTypes.SIMPLE_EVENT,
                SimpleEventPacket.Subtype.PAUSE,
            ),
            Triple(
                PlayerShipDamagePacket::class,
                TestPacketTypes.SIMPLE_EVENT,
                SimpleEventPacket.Subtype.PLAYER_SHIP_DAMAGE,
            ),
            Triple(
                VersionPacket::class,
                TestPacketTypes.CONNECTED,
                0.toByte(),
            ),
            Triple(
                WelcomePacket::class,
                TestPacketTypes.PLAIN_TEXT_GREETING,
                0.toByte(),
            ),
        )

        describe("Can register SERVER packet types") {
            withData(
                nameFn = {
                    val name = it.first.java.simpleName
                    if (name == "Server") "HeartbeatPacket.Server" else name
                },
                allServerPacketClasses,
            ) { (packetClass, type, subtype) ->
                val protocol = PacketTestProtocol(packetClass)
                protocol.getFactory(type, subtype).shouldNotBeNull()
            }
        }

        describe("Cannot register packet types without PacketReader parameter") {
            withData(
                nameFn = { it.first },
                "ActivateUpgradePacket" to ActivateUpgradePacket::class,
                "AudioCommandPacket" to AudioCommandPacket::class,
                "ButtonClickPacket" to ButtonClickPacket::class,
                "CommsOutgoingPacket" to CommsOutgoingPacket::class,
                "HeartbeatPacket.Client" to HeartbeatPacket.Client::class,
                "ReadyPacket" to ReadyPacket::class,
                "SetConsolePacket" to SetConsolePacket::class,
                "SetShipPacket" to SetShipPacket::class,
                "ToggleRedAlertPacket" to ToggleRedAlertPacket::class,
            ) {
                shouldThrow<IllegalArgumentException> { PacketTestProtocol(it.second) }
            }
        }

        describe("Cannot register RawPacket types") {
            withData(
                nameFn = { "RawPacket.${it.java.simpleName}" },
                RawPacket::class.sealedSubclasses,
            ) {
                shouldThrow<IllegalArgumentException> { PacketTestProtocol(it) }
            }
        }

        it("Cannot register unannotated packet types") {
            @Suppress("UNUSED_PARAMETER")
            class InvalidPacket(reader: PacketReader) : BaseArtemisPacket() {
                override fun writePayload(writer: PacketWriter) {
                    writer.unsupported()
                }
            }

            shouldThrow<IllegalArgumentException> { PacketTestProtocol<InvalidPacket>() }
        }

        it("Cannot register CLIENT packet types") {
            @Packet(origin = Origin.CLIENT)
            @Suppress("UNUSED_PARAMETER")
            class InvalidClientPacket(reader: PacketReader) : BaseArtemisPacket() {
                override fun writePayload(writer: PacketWriter) {
                    writer.unsupported()
                }
            }

            shouldThrow<IllegalArgumentException> { PacketTestProtocol<InvalidClientPacket>() }
        }

        it("Composite protocol") {
            Exhaustive.ints(0..allServerPacketClasses.size - 2).checkAll { i ->
                val (class1, type1, subtype1) = allServerPacketClasses[i]
                val protocol1 = PacketTestProtocol(class1)

                Exhaustive.ints(i + 1 until allServerPacketClasses.size).checkAll { j ->
                    val composite = CompositeProtocol()

                    val (class2, type2, subtype2) = allServerPacketClasses[j]
                    val protocol2 = PacketTestProtocol(class2)

                    composite.add(protocol1)
                    composite.add(protocol2)

                    composite.getFactory(type1, subtype1).shouldNotBeNull()
                    composite.getFactory(type2, subtype2).shouldNotBeNull()
                }
            }
        }
    }
})
