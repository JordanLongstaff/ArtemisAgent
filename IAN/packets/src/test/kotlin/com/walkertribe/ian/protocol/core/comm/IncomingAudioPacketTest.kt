package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.protocol.core.PacketTestFixture.Companion.writeString
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeIntLe

class IncomingAudioPacketTest : PacketTestSpec.Server<IncomingAudioPacket>(
    specName = "IncomingAudioPacket",
    fixtures = IncomingAudioPacketFixture.allFixtures(),
    failures = listOf(
        "invalid audio mode" to Arb.bind(
            Arb.int(),
            Arb.int().filter { it !in 1..2 },
        ) { id, mode ->
            buildPacket {
                writeIntLe(id)
                writeIntLe(mode)
            }
        },
        "incoming audio without title" to Arb.int().map {
            buildPacket {
                writeIntLe(it)
                writeIntLe(2)
            }
        },
        "incoming audio without filename" to Arb.bind(
            Arb.int(),
            Arb.string(),
        ) { id, title ->
            buildPacket {
                writeIntLe(id)
                writeIntLe(2)
                writeString(title)
            }
        },
    ).map { (condition, payloadGen) ->
        object : Failure(TestPacketTypes.INCOMING_MESSAGE, "Fails to parse $condition") {
            override val payloadGen: Gen<Source> = payloadGen
        }
    },
)
