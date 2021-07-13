package artemis.agent.game.missions

enum class RewardType(val parseKey: String) {
    BATTERY("batteries."),
    COOLANT("coolant."),
    NUKE("torpedoes."),
    PRODUCTION("speed."),
    SHIELD("generators.")
}
