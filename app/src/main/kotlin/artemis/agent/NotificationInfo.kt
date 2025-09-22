package artemis.agent

data class NotificationInfo(
    val channel: NotificationChannelTag,
    val title: String,
    val message: String,
    val ongoing: Boolean = false,
)
