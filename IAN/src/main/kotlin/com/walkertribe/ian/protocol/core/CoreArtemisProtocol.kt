package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.AbstractProtocol
import com.walkertribe.ian.protocol.core.comm.CommsButtonPacket
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.protocol.core.comm.IncomingAudioPacket
import com.walkertribe.ian.protocol.core.setup.AllShipSettingsPacket
import com.walkertribe.ian.protocol.core.setup.VersionPacket
import com.walkertribe.ian.protocol.core.setup.WelcomePacket
import com.walkertribe.ian.protocol.core.world.BiomechRagePacket
import com.walkertribe.ian.protocol.core.world.DeleteObjectPacket
import com.walkertribe.ian.protocol.core.world.DockedPacket
import com.walkertribe.ian.protocol.core.world.IntelPacket
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacket

/**
 * Implements the core Artemis protocol.
 * @author rjwut
 */
class CoreArtemisProtocol : AbstractProtocol() {
    init {
        arrayOf(
            // server packets
            AllShipSettingsPacket::class,
            BayStatusPacket::class,
            BiomechRagePacket::class,
            CommsButtonPacket::class,
            CommsIncomingPacket::class,
            DeleteObjectPacket::class,
            DockedPacket::class,
            EndGamePacket::class,
            GameOverReasonPacket::class,
            GameStartPacket::class,
            HeartbeatPacket.Server::class,
            IncomingAudioPacket::class,
            IntelPacket::class,
            JumpEndPacket::class,
            ObjectUpdatePacket::class,
            PausePacket::class,
            PlayerShipDamagePacket::class,
            VersionPacket::class,
            WelcomePacket::class,
        ).forEach { register(it) }
    }
}
