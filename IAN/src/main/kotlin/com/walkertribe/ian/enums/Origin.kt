package com.walkertribe.ian.enums

/**
 * Represents the type of the machine found at the opposite end of a connection.
 * @author rjwut
 */
enum class Origin {
    SERVER,
    CLIENT;

    /**
     * Returns the Int value for this Origin.
     */
    fun toInt(): Int = ordinal + 1

    companion object {
        /**
         * Returns the Origin that corresponds to the given Int value.
         */
        operator fun get(value: Int): Origin? = when (value) {
            1 -> SERVER
            2 -> CLIENT
            else -> null
        }
    }
}
