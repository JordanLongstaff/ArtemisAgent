package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.exhaustive.bytes
import io.kotest.property.exhaustive.filterNot
import io.kotest.property.exhaustive.map
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source

class CommsButtonPacketTest : PacketTestSpec.Server<CommsButtonPacket>(
    specName = "CommsButtonPacket",
    fixtures = CommsButtonPacketFixture.allFixtures(),
    failures = listOf(
        object : Failure(TestPacketTypes.COMMS_BUTTON, "Fails to parse invalid action") {
            override val payloadGen: Gen<Source> = Exhaustive.bytes().filterNot {
                CommsButtonPacketFixture.ALL_VALID_ACTIONS.contains(it)
            }.map {
                buildPacket { writeByte(it) }
            }
        }
    ),
)
