package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.PacketException
import com.walkertribe.ian.util.Version
import kotlinx.io.IOException

/**
 * Indicates the reason that the connection was terminated.
 */
sealed interface DisconnectCause {
    /**
     * The connection was closed from this side; in other words, the user terminated the connection
     * intentionally.
     */
    data object LocalDisconnect : DisconnectCause

    /**
     * The connection was closed from the remote side. This could be because the remote machine
     * terminated the connection intentionally, or a network problem caused the connection to drop.
     */
    data object RemoteDisconnect : DisconnectCause

    /**
     * IAN encountered an error from which it could not recover while attempting to parse a packet.
     * This would typically be caused by a bug in IAN.
     */
    data class PacketParseError(val exception: PacketException) : DisconnectCause

    /**
     * An I/O exception occurred. The [exception] property may have more information, but this is
     * generally an external problem that IAN can't do anything about.
     */
    data class IOError(val exception: IOException) : DisconnectCause

    /**
     * An unknown error occurred. The [throwable] property may have more information, but this is
     * generally an external problem that IAN can't do anything about.
     */
    data class UnknownError(val throwable: Throwable) : DisconnectCause

    /**
     * The server version in use is not supported by IAN.
     */
    data class UnsupportedVersion(val version: Version) : DisconnectCause
}
