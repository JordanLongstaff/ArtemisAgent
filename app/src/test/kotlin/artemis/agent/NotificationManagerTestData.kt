package artemis.agent

data class NotificationTestChannel(
    val id: String,
    val name: String,
    val nameRes: Int,
    val importance: Int,
)

class NotificationTestChannelGroup(
    val id: String,
    val name: String,
    val nameRes: Int,
    vararg val channels: NotificationTestChannel,
)
