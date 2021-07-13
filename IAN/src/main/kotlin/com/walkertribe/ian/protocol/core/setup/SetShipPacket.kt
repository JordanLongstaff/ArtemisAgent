package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.ValueIntPacket
import com.walkertribe.ian.world.Artemis

/**
 * Set the ship you want to be on. You must send this packet before
 * SetConsolePacket.
 * @author dhleong
 */
@Packet(
    origin = Origin.CLIENT,
    type = CorePacketType.VALUE_INT,
    subtype = ValueIntPacket.Subtype.SET_SHIP
)
class SetShipPacket(
    /**
     * The ship index being selected (0-based).
     */
    val shipIndex: Int
) : ValueIntPacket(shipIndex) {
    init {
        require(shipIndex in 0 until Artemis.SHIP_COUNT) {
            "Ship index must be greater than -1 and less than ${Artemis.SHIP_COUNT}"
        }
    }
}
