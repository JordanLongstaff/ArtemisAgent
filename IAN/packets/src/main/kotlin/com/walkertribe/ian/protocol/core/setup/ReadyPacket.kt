package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.protocol.core.ValueIntPacket

/**
 * Signals to the server that this console is ready to join the game. If the [ReadyPacket] is sent
 * before the game has started, the server will start sending updates when the game starts. If the
 * [ReadyPacket] is sent after the game has started, the server sends updates immediately.
 *
 * @author dhleong
 */
class ReadyPacket : ValueIntPacket(Subtype.READY, 0)
