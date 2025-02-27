package com.walkertribe.ian.protocol

import com.walkertribe.ian.protocol.core.BayStatusPacket
import com.walkertribe.ian.protocol.core.EndGamePacket
import com.walkertribe.ian.protocol.core.GameOverReasonPacket
import com.walkertribe.ian.protocol.core.GameStartPacket
import com.walkertribe.ian.protocol.core.HeartbeatPacket
import com.walkertribe.ian.protocol.core.JumpEndPacket
import com.walkertribe.ian.protocol.core.PausePacket
import com.walkertribe.ian.protocol.core.PlayerShipDamagePacket
import com.walkertribe.ian.protocol.core.SimpleEventPacket
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.protocol.core.comm.CommsButtonPacket
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.protocol.core.comm.IncomingAudioPacket
import com.walkertribe.ian.protocol.core.setup.AllShipSettingsPacket
import com.walkertribe.ian.protocol.core.setup.VersionPacket
import com.walkertribe.ian.protocol.core.setup.WelcomePacket
import com.walkertribe.ian.protocol.core.world.BiomechRagePacket
import com.walkertribe.ian.protocol.core.world.DeleteObjectPacket
import com.walkertribe.ian.protocol.core.world.DockedPacket
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacket
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.koin.KoinExtension
import io.kotest.koin.KoinLifecycleMode
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.bytes
import io.kotest.property.exhaustive.filterNot
import io.kotest.property.exhaustive.of
import kotlin.reflect.KClass
import org.koin.ksp.generated.defaultModule
import org.koin.test.KoinTest
import org.koin.test.inject

class ProtocolTest : DescribeSpec(), KoinTest {
    private val protocol: Protocol by inject()

    override fun extensions(): List<Extension> =
        listOf(KoinExtension(defaultModule, mode = KoinLifecycleMode.Root))

    init {
        describe("Protocol") {
            describe("Has factories registered for server packets") {
                withData(
                    nameFn = { it.packetName },
                    ServerPacketRegistration(
                        AllShipSettingsPacket::class,
                        type = TestPacketTypes.SIMPLE_EVENT,
                        subtype = 0x0f,
                    ),
                    ServerPacketRegistration(
                        BayStatusPacket::class,
                        type = TestPacketTypes.CARRIER_RECORD,
                    ),
                    ServerPacketRegistration(
                        BiomechRagePacket::class,
                        type = TestPacketTypes.SIMPLE_EVENT,
                        subtype = 0x19,
                    ),
                    ServerPacketRegistration(
                        CommsButtonPacket::class,
                        type = TestPacketTypes.COMMS_BUTTON,
                    ),
                    ServerPacketRegistration(
                        CommsIncomingPacket::class,
                        type = TestPacketTypes.COMM_TEXT,
                    ),
                    ServerPacketRegistration(
                        DeleteObjectPacket::class,
                        type = TestPacketTypes.OBJECT_DELETE,
                    ),
                    ServerPacketRegistration(
                        DockedPacket::class,
                        type = TestPacketTypes.SIMPLE_EVENT,
                        subtype = 0x1a,
                    ),
                    ServerPacketRegistration(
                        EndGamePacket::class,
                        type = TestPacketTypes.SIMPLE_EVENT,
                        subtype = 0x06,
                    ),
                    ServerPacketRegistration(
                        GameOverReasonPacket::class,
                        type = TestPacketTypes.SIMPLE_EVENT,
                        subtype = 0x14,
                    ),
                    ServerPacketRegistration(
                        GameStartPacket::class,
                        type = TestPacketTypes.START_GAME,
                    ),
                    ServerPacketRegistration(
                        HeartbeatPacket.Server::class,
                        packetName = "HeartbeatPacket.Server",
                        type = TestPacketTypes.HEARTBEAT,
                    ),
                    ServerPacketRegistration(
                        IncomingAudioPacket::class,
                        type = TestPacketTypes.INCOMING_MESSAGE,
                    ),
                    ServerPacketRegistration(
                        JumpEndPacket::class,
                        type = TestPacketTypes.SIMPLE_EVENT,
                        subtype = 0x0d,
                    ),
                    ServerPacketRegistration(
                        ObjectUpdatePacket::class,
                        type = TestPacketTypes.OBJECT_BIT_STREAM,
                    ),
                    ServerPacketRegistration(
                        PausePacket::class,
                        type = TestPacketTypes.SIMPLE_EVENT,
                        subtype = 0x04,
                    ),
                    ServerPacketRegistration(
                        PlayerShipDamagePacket::class,
                        type = TestPacketTypes.SIMPLE_EVENT,
                        subtype = 0x05,
                    ),
                    ServerPacketRegistration(
                        VersionPacket::class,
                        type = TestPacketTypes.CONNECTED,
                    ),
                    ServerPacketRegistration(
                        WelcomePacket::class,
                        type = TestPacketTypes.PLAIN_TEXT_GREETING,
                    ),
                ) {
                    val subtypeGen: Gen<Byte> =
                        it.subtype?.let { byte -> Exhaustive.of(byte) } ?: Exhaustive.bytes()
                    subtypeGen.checkAll { subtype ->
                        val factory = protocol.getFactory(it.type, subtype)
                        factory.shouldNotBeNull()
                        factory.factoryClass shouldBeEqual it.packetClass
                    }
                }
            }

            describe("Does not have factories registered for client packets") {
                withData(
                    nameFn = { it.packetName },
                    ClientPacketTestCase("ActivateUpgradePacket.Current", 0x1c),
                    ClientPacketTestCase("ActivateUpgradePacket.Old", 0x1b),
                    ClientPacketTestCase(
                        "AudioCommandMessage",
                        type = TestPacketTypes.CONTROL_MESSAGE,
                    ),
                    ClientPacketTestCase("ButtonClickPacket", 0x15),
                    ClientPacketTestCase(
                        "CommsOutgoingPacket",
                        type = TestPacketTypes.COMMS_MESSAGE,
                    ),
                    ClientPacketTestCase("HeartbeatPacket.Client", 0x24),
                    ClientPacketTestCase("ReadyPacket", 0x0f),
                    ClientPacketTestCase("SetConsolePacket", 0x0e),
                    ClientPacketTestCase("SetShipPacket", 0x0d),
                    ClientPacketTestCase("ToggleRedAlertPacket", 0x0a),
                ) {
                    val subtypeGen: Gen<Byte> =
                        it.subtype?.let { byte -> Exhaustive.of(byte) } ?: Exhaustive.bytes()
                    subtypeGen.checkAll { subtype ->
                        val factory = protocol.getFactory(it.type, subtype)
                        factory.shouldBeNull()
                    }
                }
            }

            describe("Does not have factories registered for arbitrary packet types") {
                it("Simple event packets") {
                    val simpleEventSubtypes =
                        setOf(
                            SimpleEventPacket.Subtype.PAUSE,
                            SimpleEventPacket.Subtype.PLAYER_SHIP_DAMAGE,
                            SimpleEventPacket.Subtype.END_GAME,
                            SimpleEventPacket.Subtype.JUMP_END,
                            SimpleEventPacket.Subtype.SHIP_SETTINGS,
                            SimpleEventPacket.Subtype.GAME_OVER_REASON,
                            SimpleEventPacket.Subtype.BIOMECH_STANCE,
                            SimpleEventPacket.Subtype.DOCKED,
                        )

                    Exhaustive.bytes().filterNot(simpleEventSubtypes::contains).checkAll {
                        protocol.getFactory(TestPacketTypes.SIMPLE_EVENT, it).shouldBeNull()
                    }
                }

                it("Other packets") {
                    val registeredPacketTypes =
                        setOf(
                            TestPacketTypes.CARRIER_RECORD,
                            TestPacketTypes.COMMS_BUTTON,
                            TestPacketTypes.COMM_TEXT,
                            TestPacketTypes.CONNECTED,
                            TestPacketTypes.HEARTBEAT,
                            TestPacketTypes.INCOMING_MESSAGE,
                            TestPacketTypes.OBJECT_BIT_STREAM,
                            TestPacketTypes.OBJECT_DELETE,
                            TestPacketTypes.PLAIN_TEXT_GREETING,
                            TestPacketTypes.SIMPLE_EVENT,
                            TestPacketTypes.START_GAME,
                        )

                    checkAll(
                        Arb.int().filterNot(registeredPacketTypes::contains),
                        Exhaustive.bytes(),
                    ) { type, subtype ->
                        protocol.getFactory(type, subtype).shouldBeNull()
                    }
                }
            }
        }
    }

    data class ServerPacketRegistration(
        val packetClass: KClass<out Packet>,
        val packetName: String = packetClass.java.simpleName,
        val type: Int,
        val subtype: Byte? = null,
    )

    data class ClientPacketTestCase(
        val packetName: String,
        val subtype: Byte? = null,
        val type: Int = TestPacketTypes.VALUE_INT,
    )
}
