package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.world.Artemis
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.negativeInt
import io.kotest.property.checkAll

class SetShipPacketTest :
    PacketTestSpec.Client<SetShipPacket>(
        specName = "SetShipPacket",
        fixtures = SetShipPacketFixture.ALL,
    ) {
    override suspend fun DescribeSpecContainerScope.describeMore() {
        describe("Invalid ship index throws") {
            withData(
                nameFn = { it.first },
                "Negative" to Arb.negativeInt(),
                "Too high" to Arb.int(min = Artemis.SHIP_COUNT),
            ) { (_, arbInt) ->
                arbInt.checkAll { shouldThrow<IllegalArgumentException> { SetShipPacket(it) } }
            }
        }
    }
}
