package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.ValueIntPacket

/**
 * Toggles red alert on and off.
 */
@Packet(
    origin = Origin.CLIENT,
    type = CorePacketType.VALUE_INT,
    subtype = ValueIntPacket.Subtype.TOGGLE_RED_ALERT
)
class ToggleRedAlertPacket : ValueIntPacket(0)
