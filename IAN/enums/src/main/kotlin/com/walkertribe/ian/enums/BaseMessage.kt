package com.walkertribe.ian.enums

/**
 * Messages that can be sent to bases.
 *
 * @author rjwut
 */
sealed class BaseMessage(override val id: Int) : CommsMessage {
    data object StandByForDockingOrCeaseOperation : BaseMessage(0)

    data object PleaseReportStatus : BaseMessage(1)

    class Build private constructor(val ordnanceType: OrdnanceType) :
        BaseMessage(ordnanceType.ordinal + 2) {
        override fun toString(): String = "Build$ordnanceType"

        companion object {
            private val BUILD_MESSAGES = OrdnanceType.entries.map(::Build)

            operator fun invoke(ordnanceType: OrdnanceType): Build =
                BUILD_MESSAGES[ordnanceType.ordinal]
        }
    }

    override val recipientType: CommsRecipientType
        get() = CommsRecipientType.BASE
}
