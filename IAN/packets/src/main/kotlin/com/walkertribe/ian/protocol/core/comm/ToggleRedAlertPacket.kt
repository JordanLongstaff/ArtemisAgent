package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.protocol.core.ValueIntPacket

/** Toggles red alert on and off. */
class ToggleRedAlertPacket : ValueIntPacket(Subtype.TOGGLE_RED_ALERT, 0)
