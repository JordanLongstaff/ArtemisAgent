package artemis.agent

enum class NotificationChannelTag {
    GAME_INFO,
    CONNECTION,
    GAME_OVER,
    BORDER_WAR,
    DEEP_STRIKE,
    NEW_MISSION,
    MISSION_PROGRESS,
    MISSION_COMPLETED,
    PRODUCTION,
    ATTACK,
    DESTROYED,
    REANIMATE,
    PERFIDY;

    val tag = name.lowercase().replace('_', ' ')
}
