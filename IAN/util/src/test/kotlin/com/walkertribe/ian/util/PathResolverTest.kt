package com.walkertribe.ian.util

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import okio.IOException
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.io.File

class PathResolverTest : DescribeSpec({
    val tmpFile = tempfile()
    val tmpDir = tempdir()
    val tmpDirPath = tmpDir.toOkioPath()

    describe("PathResolver") {
        it("Path dat/") {
            PathResolver.DAT.name shouldBeEqual "dat"
        }
    }

    describe("FilePathResolver") {
        it("Can create") {
            FilePathResolver(tmpDirPath)
        }

        it("Can create input stream from file") {
            File(tmpDir, "foo").createNewFile()
            val pathResolver = FilePathResolver(tmpDirPath)
            shouldNotThrow<IOException> {
                pathResolver("foo".toPath()) {
                    readUtf8()
                }.shouldNotBeNull().shouldBeInstanceOf<String>()
            }
        }

        it("Throws if parent file is not a directory") {
            shouldThrow<IllegalArgumentException> { FilePathResolver(tmpFile.toOkioPath()) }
        }

        it("Throws if directory does not exist") {
            shouldThrow<IllegalArgumentException> { FilePathResolver(tmpDirPath / "bar") }
        }
    }
})
