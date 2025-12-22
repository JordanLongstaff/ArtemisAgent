package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.triple
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeFloatLe

class GridUpdatePacketTest :
    PacketTestSpec.Server<GridUpdatePacket>(
        specName = "GridUpdatePacket",
        fixtures = GridUpdatePacketFixture.ALL,
        failures =
            listOf(
                    "invalid X-coordinate" to
                        Arb.triple(
                            Arb.byte().filter { it !in -1..<5 },
                            Arb.byte(0, 5),
                            Arb.byte(0, 10),
                        ),
                    "invalid Y-coordinate" to
                        Arb.triple(
                            Arb.byte(0, 5),
                            Arb.byte().filter { it !in 0..<5 },
                            Arb.byte(0, 10),
                        ),
                    "invalid Z-coordinate" to
                        Arb.triple(
                            Arb.byte(0, 5),
                            Arb.byte(0, 5),
                            Arb.byte().filter { it !in 0..<10 },
                        ),
                )
                .map { (condition, coordGen) ->
                    object :
                        Failure(TestPacketTypes.SHIP_SYSTEM_SYNC, "Fails to parse $condition") {
                        override val payloadGen: Gen<Source> =
                            Arb.bind(Arb.byte(), coordGen, Arb.float()) { full, (x, y, z), damage ->
                                buildPacket {
                                    writeByte(full)
                                    writeByte(x)
                                    writeByte(y)
                                    writeByte(z)
                                    writeFloatLe(damage)
                                }
                            }
                    }
                },
    )
