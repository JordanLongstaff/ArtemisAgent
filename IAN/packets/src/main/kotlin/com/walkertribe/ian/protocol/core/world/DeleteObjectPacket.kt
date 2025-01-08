package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketType
import com.walkertribe.ian.protocol.core.CorePacketType

/**
 * Sent by the server when an object is deleted from the simulation. This doesn't necessarily mean
 * that an explosion effect should be shown.
 */
@PacketType(type = CorePacketType.OBJECT_DELETE)
class DeleteObjectPacket(reader: PacketReader) : Packet.Server(reader) {
    /** The ObjectType of the deleted object */
    val targetType: ObjectType =
        reader.readByte().toInt().let {
            requireNotNull(ObjectType[it]) { "Invalid object type: $it" }
        }

    /** The deleted object's ID */
    val target: Int = reader.readInt().also(reader::acceptObjectID)
}
