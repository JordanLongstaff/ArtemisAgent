package com.walkertribe.ian.vesseldata

enum class TestFactionAttributes(vararg val expected: String) {
    PLAYER(Key.PLAYER),
    PLAYER_JUMPMASTER(Key.PLAYER, Key.JUMPMASTER),
    FRIENDLY(Key.FRIENDLY),
    ENEMY_STANDARD(Key.ENEMY, Key.STANDARD),
    ENEMY_SUPPORT_WHALELOVER(Key.ENEMY, Key.SUPPORT, Key.WHALELOVER),
    ENEMY_SUPPORT_WHALEHATER(Key.ENEMY, Key.SUPPORT, Key.WHALEHATER),
    ENEMY_LONER_HASSPECIALS(Key.ENEMY, Key.LONER, Key.HASSPECIALS),
    ENEMY_LONER_BIOMECH(Key.ENEMY, Key.LONER, Key.BIOMECH);

    val keys: String by lazy { name.replace('_', ' ').lowercase() }

    private object Key {
        const val BIOMECH = "biomech"
        const val ENEMY = "enemy"
        const val FRIENDLY = "friendly"
        const val HASSPECIALS = "hasspecials"
        const val JUMPMASTER = "jumpmaster"
        const val LONER = "loner"
        const val PLAYER = "player"
        const val STANDARD = "standard"
        const val SUPPORT = "support"
        const val WHALEHATER = "whalehater"
        const val WHALELOVER = "whalelover"
    }
}
