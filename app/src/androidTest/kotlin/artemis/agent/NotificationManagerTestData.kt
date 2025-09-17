package artemis.agent

data class NotificationTestChannel(val id: String, val name: String, val importance: Int)

class NotificationTestChannelGroup(
    val id: String,
    val name: String,
    vararg val channels: NotificationTestChannel,
)
