package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.iface.ListenerModule
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketType
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.world.ArtemisObject
import kotlin.reflect.KClass

/**
 * A packet which contains updates for world objects.
 *
 * While the `ObjectUpdatePacket` supports payloads with heterogeneous object type, in practice the
 * Artemis server only sends packets with homogeneous types; in other words, object updates of
 * different types are sent in separate packets. Initial tests seem to indicate that the stock
 * Artemis client can handle heterogeneous update types in a single packet, but this is not yet
 * considered 100% confirmed. If you wish to ensure that you completely emulate the behavior of an
 * Artemis server, send separate packets for separate object types.
 *
 * The `ArtemisPlayer` object is actually expressed in four different update types, depending on the
 * data that it contains:
 * * `ObjectType.PLAYER`: Data not included in the other three types
 * * `ObjectType.WEAPONS_CONSOLE`: Data about ordnance counts and tube status
 * * `ObjectType.ENGINEERING_CONSOLE`: Data about system status (energy, heat, coolant)
 * * `ObjectType.UPGRADES`: Data about upgrade status
 *
 * @author rjwut
 */
@PacketType(type = CorePacketType.OBJECT_BIT_STREAM)
class ObjectUpdatePacket(reader: PacketReader) : Packet.Server(reader) {
    private companion object {
        private val PARSERS =
            listOf(
                    PlayerShipParser,
                    WeaponsParser,
                    UnobservedObjectParser.Engineering,
                    UpgradesParser,
                    NpcShipParser,
                    BaseParser,
                    MineParser,
                    UnobservedObjectParser.Anomaly,
                    UnobservedObjectParser.Nebula,
                    UnobservedObjectParser.Torpedo,
                    BlackHoleParser,
                    UnobservedObjectParser.Asteroid,
                    UnobservedObjectParser.GenericMesh,
                    CreatureParser,
                    UnobservedObjectParser.Drone,
                )
                .associateBy { it.objectType.id }
    }

    /** Returns the updated objects. */
    val objects: List<ArtemisObject<*>>

    /** Returns the types of updated objects. */
    val objectClasses: Set<KClass<out ArtemisObject<*>>>

    init {
        val objectList = mutableListOf<ArtemisObject<*>>()
        val classes = mutableSetOf<KClass<out ArtemisObject<*>>>()

        do {
            val objectType = reader.peekByte()
            if (objectType == 0.toByte()) break

            val parser = requireNotNull(PARSERS[objectType]) { "Invalid object type: $objectType" }
            parser.parse(reader, timestamp)?.also {
                objectList.add(it)
                classes.add(it::class)
            }
        } while (true)
        reader.skip(Int.SIZE_BYTES)

        objects = objectList
        objectClasses = classes
    }

    override fun offerTo(module: ListenerModule) {
        super.offerTo(module)
        objects.forEach { it.offerTo(module) }
    }
}
