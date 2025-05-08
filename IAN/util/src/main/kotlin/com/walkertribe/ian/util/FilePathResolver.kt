package com.walkertribe.ian.util

import okio.BufferedSource
import okio.FileSystem
import okio.IOException
import okio.Path

/**
 * An implementation of PathResolver that reads resources relative to a specified directory on disk.
 * This should be the Artemis install directory, or another directory that contains the appropriate
 * resources in the same paths.
 *
 * @author rjwut
 */
class FilePathResolver(private val directory: Path) : PathResolver {
    init {
        FileSystem.SYSTEM.apply {
            require(exists(directory)) { "Directory does not exist" }
            require(metadata(directory).isDirectory) { "Not a directory" }
        }
    }

    @Throws(IOException::class)
    override fun <T> invoke(path: Path, readerAction: BufferedSource.() -> T): T =
        FileSystem.SYSTEM.read(directory / path, readerAction)
}
