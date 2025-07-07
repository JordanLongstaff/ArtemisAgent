package com.walkertribe.ian.enums

/**
 * Messages that can be sent to civilian NPCs.
 *
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

    data class GoDefend(val targetID: Int) : OtherMessage(DEFEND)

    override val recipientType: CommsRecipientType
        get() = CommsRecipientType.OTHER

    companion object {
        private const val HAIL = 0
        private const val TURN_0 = 1
        private const val TURN_90 = 2
        private const val TURN_180 = 3
        private const val TURN_270 = 4
        private const val TURN_LEFT_10 = 5
        private const val TURN_LEFT_25 = 15
        private const val TURN_RIGHT_10 = 6
        private const val TURN_RIGHT_25 = 16
        private const val ATTACK = 7
        private const val PROCEED = 8
        private const val DEFEND = 9
    }
}
