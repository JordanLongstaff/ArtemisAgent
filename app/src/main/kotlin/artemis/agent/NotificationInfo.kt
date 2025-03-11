package artemis.agent

data class NotificationInfo(
    val channelId: String,
    val title: String,
    val message: String,
    val ongoing: Boolean = false,
)
