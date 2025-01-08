package com.walkertribe.ian.util

import okio.BufferedSource
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

/**
 * A PathResolver is an object which can accept a path to a particular resource and return an
 * InputStream to it. This is used by IAN when it needs to read in a resource. Note that all
 * resource paths are expressed relative to the Artemis install directory.
 *
 * @author rjwut
 */
interface PathResolver {
    /** Returns an InputStream from which the data at the given path can be read. */
    @Throws(IOException::class)
    operator fun <T> invoke(path: Path, readerAction: BufferedSource.() -> T): T

    companion object {
        val DAT = "dat".toPath()
    }
}
