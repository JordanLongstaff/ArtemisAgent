package com.walkertribe.ian.enums

/**
 * Represents the type of the machine found at the opposite end of a connection.
 *
 * @author rjwut
 */
@JvmInline
value class Origin(val value: Int) {
    val isValid: Boolean
        get() = entries.contains(this)

    companion object {
        val SERVER = Origin(1)
        val CLIENT = Origin(2)

        val entries = setOf(SERVER, CLIENT)
    }
}
