package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.vesseldata.Empty
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.world.ArtemisBlackHole
import com.walkertribe.ian.world.ArtemisCreature
import com.walkertribe.ian.world.ArtemisMine
import com.walkertribe.ian.world.ArtemisPlayer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.checkAll
import kotlinx.coroutines.launch

class CommsOutgoingPacketTest :
    PacketTestSpec.Client<CommsOutgoingPacket>(
        specName = "CommsOutgoingPacket",
        fixtures = CommsOutgoingPacketFixture.ALL,
    ) {
    override fun DescribeSpecContainerScope.describeMore() = launch {
        describe("Throws with invalid arguments") {
            val castFixtures = fixtures.filterIsInstance<CommsOutgoingPacketFixture>()

            describe("Invalid recipient object") {
                withData(
                    nameFn = { it.first },
                    "ArtemisBlackHole" to Arb.bind<ArtemisBlackHole>(),
                    "ArtemisCreature" to Arb.bind<ArtemisCreature>(),
                    "ArtemisMine" to Arb.bind<ArtemisMine>(),
                    "ArtemisPlayer" to Arb.bind<ArtemisPlayer>(),
                ) { (_, arbObject) ->
                    castFixtures
                        .distinctBy { it.expectedRecipientType }
                        .forEach { fixture ->
                            checkAll(arbObject, fixture.messageGen) { recipient, message ->
                                shouldThrow<IllegalArgumentException> {
                                    CommsOutgoingPacket(recipient, message, VesselData.Empty)
                                }
                            }
                        }
                }
            }

            describe("Recipient-message type mismatch") {
                val mainFixtures =
                    listOf(
                        CommsOutgoingPacketFixture.Enemy,
                        CommsOutgoingPacketFixture.Base,
                        CommsOutgoingPacketFixture.Other,
                    )

                withData(nameFn = { "${it.expectedRecipientType} message" }, mainFixtures) {
                    fixture1 ->
                    withData(
                        nameFn = { "${it.expectedRecipientType} recipient" },
                        mainFixtures.filter {
                            it.expectedRecipientType != fixture1.expectedRecipientType
                        },
                    ) { fixture2 ->
                        checkAll(fixture2.recipientGen, fixture1.messageGen) { recipient, message ->
                            shouldThrow<IllegalArgumentException> {
                                CommsOutgoingPacket(recipient, message, VesselData.Empty)
                            }
                        }
                    }
                }
            }
        }
    }
}
