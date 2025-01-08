package com.walkertribe.ian.enums

/**
 * The types of audio notification that the comm officer can receive.
 *
 * @author rjwut
 */
sealed interface AudioMode {
    data object Playing : AudioMode

    data class Incoming(val title: String, val filename: String) : AudioMode
}
