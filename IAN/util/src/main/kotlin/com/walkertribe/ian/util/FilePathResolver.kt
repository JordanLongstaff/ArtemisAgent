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
    @Throws(IOException::class)
    override fun <T> invoke(path: Path, readerAction: BufferedSource.() -> T): T =
        fileSystem.read(directory / path, readerAction)

    init {
        require(fileSystem.exists(directory)) { "Directory does not exist" }
        require(fileSystem.metadata(directory).isDirectory) { "Not a directory" }
    }

    private companion object {
        private val fileSystem = FileSystem.SYSTEM
    }
}
