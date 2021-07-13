package com.walkertribe.ian.enums

import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.world.ArtemisObject

/**
 * Messages that can be sent to civilian NPCs.
 * @author rjwut
 */
sealed class OtherMessage(override val id: Int) : CommsMessage {
    data object Hail : OtherMessage(HAIL)
    data object TurnToHeading0 : OtherMessage(TURN_0)
    data object TurnToHeading90 : OtherMessage(TURN_90)
    data object TurnToHeading180 : OtherMessage(TURN_180)
    data object TurnToHeading270 : OtherMessage(TURN_270)
    data object TurnLeft10Degrees : OtherMessage(TURN_LEFT_10)
    data object TurnRight10Degrees : OtherMessage(TURN_RIGHT_10)
    data object TurnLeft25Degrees : OtherMessage(TURN_LEFT_25)
    data object TurnRight25Degrees : OtherMessage(TURN_RIGHT_25)
    data object AttackNearestEnemy : OtherMessage(ATTACK)
    data object ProceedToYourDestination : OtherMessage(PROCEED)
    data class GoDefend internal constructor(override val argument: Int) : OtherMessage(DEFEND) {
        constructor(target: ArtemisObject) : this(target.id)
    }

    internal open val argument: Int get() = CommsMessage.NO_ARG

    override val recipientType: CommsRecipientType = CommsRecipientType.OTHER

    override fun writeTo(writer: PacketWriter) {
        writer.writeInt(id).writeInt(argument)
    }

    private companion object {
        const val HAIL = 0
        const val TURN_0 = 1
        const val TURN_90 = 2
        const val TURN_180 = 3
        const val TURN_270 = 4
        const val TURN_LEFT_10 = 5
        const val TURN_LEFT_25 = 15
        const val TURN_RIGHT_10 = 6
        const val TURN_RIGHT_25 = 16
        const val ATTACK = 7
        const val PROCEED = 8
        const val DEFEND = 9
    }
}
