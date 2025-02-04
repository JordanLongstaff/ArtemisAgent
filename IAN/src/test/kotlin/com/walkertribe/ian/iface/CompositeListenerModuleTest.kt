package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.BayStatusPacket
import com.walkertribe.ian.protocol.core.EndGamePacket
import com.walkertribe.ian.protocol.core.GameOverReasonPacket
import com.walkertribe.ian.protocol.core.GameStartPacket
import com.walkertribe.ian.protocol.core.HeartbeatPacket
import com.walkertribe.ian.protocol.core.JumpEndPacket
import com.walkertribe.ian.protocol.core.PausePacket
import com.walkertribe.ian.protocol.core.PlayerShipDamagePacket
import com.walkertribe.ian.protocol.core.SimpleEventPacket
import com.walkertribe.ian.protocol.core.comm.CommsButtonPacket
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.protocol.core.comm.IncomingAudioPacket
import com.walkertribe.ian.protocol.core.setup.AllShipSettingsPacket
import com.walkertribe.ian.protocol.core.setup.VersionPacket
import com.walkertribe.ian.protocol.core.setup.WelcomePacket
import com.walkertribe.ian.protocol.core.world.BiomechRagePacket
import com.walkertribe.ian.protocol.core.world.DeleteObjectPacket
import com.walkertribe.ian.protocol.core.world.DockedPacket
import com.walkertribe.ian.protocol.core.world.IntelPacket
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacket
import com.walkertribe.ian.world.ArtemisBase
import com.walkertribe.ian.world.ArtemisBlackHole
import com.walkertribe.ian.world.ArtemisCreature
import com.walkertribe.ian.world.ArtemisMine
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisObject
import com.walkertribe.ian.world.ArtemisPlayer
import com.walkertribe.ian.world.ArtemisShielded
import com.walkertribe.ian.world.BaseArtemisObject
import com.walkertribe.ian.world.BaseArtemisShielded
import com.walkertribe.ian.world.BaseArtemisShip
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.property.Arb
import io.kotest.property.arbitrary.subsequence
import io.kotest.property.checkAll
import io.mockk.mockk

class CompositeListenerModuleTest :
    DescribeSpec({
        describe("CompositeListenerModule") {
            val connectionEventClasses =
                listOf(
                    ConnectionEvent::class,
                    ConnectionEvent.Success::class,
                    ConnectionEvent.Disconnect::class,
                    ConnectionEvent.HeartbeatLost::class,
                    ConnectionEvent.HeartbeatRegained::class,
                )

            val packetClasses =
                listOf(
                    Packet.Server::class,
                    ObjectUpdatePacket::class,
                    DeleteObjectPacket::class,
                    GameStartPacket::class,
                    PausePacket::class,
                    CommsButtonPacket::class,
                    IncomingAudioPacket::class,
                    AllShipSettingsPacket::class,
                    BayStatusPacket::class,
                    EndGamePacket::class,
                    GameOverReasonPacket::class,
                    HeartbeatPacket.Server::class,
                    JumpEndPacket::class,
                    PlayerShipDamagePacket::class,
                    CommsIncomingPacket::class,
                    BiomechRagePacket::class,
                    DockedPacket::class,
                    IntelPacket::class,
                    WelcomePacket::class,
                    VersionPacket::class,
                    SimpleEventPacket::class,
                )

            val objectClasses =
                listOf(
                    ArtemisObject::class,
                    ArtemisShielded::class,
                    BaseArtemisObject::class,
                    BaseArtemisShielded::class,
                    BaseArtemisShip::class,
                    ArtemisBase::class,
                    ArtemisBlackHole::class,
                    ArtemisCreature::class,
                    ArtemisMine::class,
                    ArtemisNpc::class,
                    ArtemisPlayer::class,
                )

            it("Accepted types") {
                checkAll(
                    Arb.subsequence(connectionEventClasses),
                    Arb.subsequence(packetClasses),
                    Arb.subsequence(objectClasses),
                ) { connectionEvents, packets, objects ->
                    CompositeListenerModule(
                            connectionEventListeners =
                                connectionEvents.map { ListenerFunction(it) {} },
                            packetListeners = packets.map { ListenerFunction(it) {} },
                            artemisObjectListeners = objects.map { ListenerFunction(it) {} },
                        )
                        .acceptedTypes
                        .shouldContainExactlyInAnyOrder(connectionEvents + packets + objects)
                }
            }

            it("Offer objects") {
                var onArtemisObjectCalled = false
                val module =
                    CompositeListenerModule(
                        connectionEventListeners = listOf(),
                        packetListeners = listOf(),
                        artemisObjectListeners =
                            listOf(
                                ListenerFunction(ArtemisObject::class) {
                                    onArtemisObjectCalled = true
                                }
                            ),
                    )
                module.onArtemisObject(mockk<ArtemisObject<*>>())
                onArtemisObjectCalled.shouldBeTrue()
            }
        }
    })
