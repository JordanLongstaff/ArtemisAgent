package artemis.agent.cpu

import artemis.agent.game.allies.AllyStatus

enum class HailResponseEffect(private val prefix: String) {
    MALFUNCTION("Our shipboard computer ha") {
        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.MALFUNCTION
    },
    HOSTAGE("We are holding this ship h") {
        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.HOSTAGE
    },
    COMMANDEERED("We have commandeered this") {
        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.COMMANDEERED
    },
    FLYING_BLIND("Our sensors are all down!") {
        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.FLYING_BLIND
    },
    AMBASSADOR("We're dead in space, our d") {
        override fun getAllyStatus(response: String): AllyStatus =
            when {
                response.substring(AMBASSADOR_SEARCH_INDEX).startsWith(PIRATE_BOSS) ->
                    AllyStatus.PIRATE_BOSS
                else -> AllyStatus.AMBASSADOR
            }
    },
    CONTRABAND("We are carrying needed su") {
        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.CONTRABAND
    },
    PIRATE_SUPPLIES("Hail, Bold Privateer!  We") {
        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.PIRATE_SUPPLIES
    },
    SECURE_DATA("We are carrying secret, s") {
        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.SECURE_DATA
    },
    PIRATE_DATA("Pirate scum!  We're carry") {
        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.PIRATE_DATA
    },
    NEEDS_DAMCON("Our engines are damaged a") {
        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.NEED_DAMCON
    },
    NEEDS_ENERGY("We're out of energy!  Cou") {
        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.NEED_ENERGY
    },
    MINE_TRAP("We're just moving cargo b") {
        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.MINE_TRAP
    },
    FIGHTER_TRAP("We're broken down!  Out o") {
        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.FIGHTER_TRAP
    },
    OTHER("") {
        private val antiprefixes = arrayOf("We appreciate your help. ", "We're heading to the stat")

        override fun appliesTo(response: String): Boolean =
            antiprefixes.none { response.startsWith(it) }

        override fun getAllyStatus(response: String): AllyStatus = AllyStatus.NORMAL
    };

    abstract fun getAllyStatus(response: String): AllyStatus

    open fun appliesTo(response: String): Boolean = response.startsWith(prefix)

    private companion object {
        const val AMBASSADOR_SEARCH_INDEX = 58
        const val PIRATE_BOSS = "the big boss"
    }
}
