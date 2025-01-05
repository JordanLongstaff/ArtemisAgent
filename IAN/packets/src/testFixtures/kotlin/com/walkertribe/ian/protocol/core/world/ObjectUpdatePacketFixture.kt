package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketTestListenerModule
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.world.ArtemisObject
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Gen

class ObjectUpdatePacketFixture private constructor(private val config: ObjectParserTestConfig) :
    PacketTestFixture.Server<ObjectUpdatePacket>(
        TestPacketTypes.OBJECT_BIT_STREAM,
        config.recognizesObjectListeners,
    ) {
    override val specName: String = config.specName
    override val groupName: String = config.parserName

    override val generator: Gen<PacketTestData.Server<ObjectUpdatePacket>>
        get() = config.dataGenerator

    val objects = mutableListOf<ArtemisObject<*>>()

    override suspend fun testType(packet: Packet.Server): ObjectUpdatePacket =
        packet.shouldBeInstanceOf()

    override suspend fun describeMore(scope: DescribeSpecContainerScope) {
        scope.it("Objects are offered to listener modules") {
            PacketTestListenerModule.objects shouldContainExactly objects
        }

        config.describeMore(scope)

        PacketTestListenerModule.objects.clear()
        objects.clear()
    }

    override fun afterTest(data: PacketTestData.Server<ObjectUpdatePacket>) {
        config.afterTest(this, data)
    }

    companion object {
        val ALL =
            listOf(
                    ObjectParserTestConfig.Empty,
                    ObjectParserTestConfig.BaseParser,
                    ObjectParserTestConfig.BlackHoleParser,
                    ObjectParserTestConfig.CreatureParser.V1,
                    ObjectParserTestConfig.CreatureParser.V2,
                    ObjectParserTestConfig.MineParser,
                    ObjectParserTestConfig.NpcShipParser.V1,
                    ObjectParserTestConfig.NpcShipParser.V2,
                    ObjectParserTestConfig.NpcShipParser.V3,
                    ObjectParserTestConfig.PlayerShipParser.V1,
                    ObjectParserTestConfig.PlayerShipParser.V2,
                    ObjectParserTestConfig.PlayerShipParser.V3,
                    ObjectParserTestConfig.PlayerShipParser.V4,
                    ObjectParserTestConfig.UpgradesParser,
                    ObjectParserTestConfig.WeaponsParser.V1,
                    ObjectParserTestConfig.WeaponsParser.V2,
                    ObjectParserTestConfig.Unobserved.Engineering,
                    ObjectParserTestConfig.Unobserved.Anomaly.V1,
                    ObjectParserTestConfig.Unobserved.Anomaly.V2,
                    ObjectParserTestConfig.Unobserved.Nebula.V1,
                    ObjectParserTestConfig.Unobserved.Nebula.V2,
                    ObjectParserTestConfig.Unobserved.Torpedo,
                    ObjectParserTestConfig.Unobserved.Asteroid,
                    ObjectParserTestConfig.Unobserved.GenericMesh.V1,
                    ObjectParserTestConfig.Unobserved.GenericMesh.V2,
                    ObjectParserTestConfig.Unobserved.Drone,
                )
                .map(::ObjectUpdatePacketFixture)
    }
}
