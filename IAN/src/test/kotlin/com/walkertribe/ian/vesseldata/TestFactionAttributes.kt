package com.walkertribe.ian.vesseldata

enum class TestFactionAttributes(vararg val expected: String) {
    PLAYER("player"),
    PLAYER_JUMPMASTER("player", "jumpmaster"),
    FRIENDLY("friendly"),
    ENEMY_STANDARD("enemy", "standard"),
    ENEMY_SUPPORT_WHALELOVER("enemy", "support", "whalelover"),
    ENEMY_SUPPORT_WHALEHATER("enemy", "support", "whalehater"),
    ENEMY_LONER_HASSPECIALS("enemy", "loner", "hasspecials"),
    ENEMY_LONER_BIOMECH("enemy", "loner", "biomech");

    val keys: String by lazy { name.replace('_', ' ').lowercase() }
}
