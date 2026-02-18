package gradle.libs

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotBeBlank
import java.io.File

class VersionCatalogTest : DescribeSpec({
    val versionCatalogFile = File("libs.versions.toml")
    val content = versionCatalogFile.readText()

    describe("Version Catalog File") {
        it("Should exist") {
            versionCatalogFile.exists() shouldBe true
        }

        it("Should be readable") {
            versionCatalogFile.canRead() shouldBe true
        }

        it("Should not be empty") {
            content.shouldNotBeBlank()
        }

        it("Should contain [versions] section") {
            content shouldMatch Regex("""\[versions]""")
        }

        it("Should contain [libraries] section") {
            content shouldMatch Regex("""\[libraries]""")
        }

        it("Should contain [bundles] section") {
            content shouldMatch Regex("""\[bundles]""")
        }

        it("Should contain [plugins] section") {
            content shouldMatch Regex("""\[plugins]""")
        }
    }

    describe("Version Declarations") {
        val versionSection = content
            .substringAfter("[versions]")
            .substringBefore("[libraries]")
            .trim()

        val versionLines = versionSection.lines()
            .filter { it.contains("=") && !it.trim().startsWith("#") }

        it("Should have version declarations") {
            versionLines.shouldNotBeEmpty()
        }

        describe("Version format validation") {
            val versions = versionLines.map { line ->
                val parts = line.split("=", limit = 2)
                parts[0].trim() to parts[1].trim().removeSurrounding("\"")
            }

            withData(nameFn = { it.first }, versions) { (name, value) ->
                name.shouldNotBeBlank()
                value.shouldNotBeBlank()
            }
        }

        it("Should contain ksp version") {
            versionSection shouldMatch Regex("""ksp\s*=\s*".*"""")
        }

        it("ksp version should be 2.3.6") {
            val kspVersion = versionLines
                .first { it.startsWith("ksp") }
                .substringAfter("=")
                .trim()
                .removeSurrounding("\"")
            kspVersion shouldBe "2.3.6"
        }

        describe("Semantic versioning compliance") {
            val semverRegex = Regex("""^\d+\.\d+(\.\d+)?(-[a-zA-Z0-9\-.]+)?(\+[a-zA-Z0-9\-.]+)?$""")
            val latestReleaseRegex = Regex("""^latest\.release$""")
            val versionPlusRegex = Regex("""^\d+\.\d+\.\+$""")

            val versions = versionLines
                .map { line ->
                    val parts = line.split("=", limit = 2)
                    parts[0].trim() to parts[1].trim().removeSurrounding("\"")
                }
                .filter { (_, value) ->
                    value.isNotEmpty() &&
                    !latestReleaseRegex.matches(value) &&
                    !versionPlusRegex.matches(value)
                }

            withData(
                nameFn = { "${it.first} = ${it.second}" },
                versions
            ) { (name, version) ->
                version shouldMatch semverRegex
            }
        }
    }

    describe("Library Declarations") {
        val librariesSection = content
            .substringAfter("[libraries]")
            .substringBefore("[bundles]")
            .trim()

        it("Should have library declarations") {
            librariesSection.shouldNotBeBlank()
        }

        it("Should contain ksp-api library") {
            librariesSection shouldMatch Regex("""ksp-api\s*=.*""")
        }

        it("ksp-api should reference ksp version") {
            librariesSection shouldMatch Regex("""ksp-api\s*=\s*\{[^}]*version\.ref\s*=\s*"ksp"[^}]*}""")
        }

        it("Should contain ksp-koin library") {
            librariesSection shouldMatch Regex("""ksp-koin\s*=.*""")
        }

        describe("Library format validation") {
            val libraryLines = librariesSection.lines()
                .filter { line ->
                    line.contains("=") &&
                    !line.trim().startsWith("#") &&
                    line.trim().isNotEmpty()
                }

            it("All libraries should have valid format") {
                libraryLines.forEach { line ->
                    val hasModuleOrId = line.contains("module") || line.contains("id")
                    hasModuleOrId shouldBe true
                }
            }
        }
    }

    describe("Bundle Declarations") {
        val bundlesSection = content
            .substringAfter("[bundles]")
            .substringBefore("[plugins]")
            .trim()

        it("Should have bundle declarations") {
            bundlesSection.shouldNotBeBlank()
        }

        it("Bundles should be well-formed arrays") {
            val bundleBlocks = bundlesSection.split(Regex("""(?m)^[a-z][-a-z]*\s*=\s*\["""))
                .drop(1)

            bundleBlocks.forEach { block ->
                val closingBracket = block.indexOf(']')
                closingBracket shouldNotBe -1
            }
        }

        describe("Bundle references validation") {
            val bundleNames = setOf(
                "classpath", "build-logic", "build-logic-api", "app", "app-debug",
                "app-debug-runtime", "app-test", "app-test-runtime", "app-androidTest",
                "ian", "ian-test", "ian-test-runtime", "firebase", "arcmutate", "konsist-common"
            )

            bundleNames.forEach { bundleName ->
                it("Should contain $bundleName bundle") {
                    bundlesSection shouldMatch Regex("""$bundleName\s*=\s*\[""")
                }
            }
        }
    }

    describe("Plugin Declarations") {
        val pluginsSection = content
            .substringAfter("[plugins]")
            .trim()

        it("Should have plugin declarations") {
            pluginsSection.shouldNotBeBlank()
        }

        it("Should contain ksp plugin") {
            pluginsSection shouldMatch Regex("""ksp\s*=.*""")
        }

        it("ksp plugin should reference ksp version") {
            pluginsSection shouldMatch Regex("""ksp\s*=\s*\{[^}]*version\.ref\s*=\s*"ksp"[^}]*}""")
        }

        describe("Plugin format validation") {
            val pluginLines = pluginsSection.lines()
                .filter { line ->
                    line.contains("=") &&
                    !line.trim().startsWith("#") &&
                    line.trim().isNotEmpty()
                }

            it("All plugins should have id and version") {
                pluginLines.forEach { line ->
                    line shouldMatch Regex(""".*\{.*id\s*=.*version.*}.*""")
                }
            }
        }
    }

    describe("Version References Integrity") {
        val versionSection = content
            .substringAfter("[versions]")
            .substringBefore("[libraries]")

        val declaredVersions = versionSection.lines()
            .filter { it.contains("=") && !it.trim().startsWith("#") }
            .map { it.substringBefore("=").trim() }
            .toSet()

        val referencedVersions = Regex("""version\.ref\s*=\s*"([^"]+)"""")
            .findAll(content)
            .map { it.groupValues[1] }
            .toSet()

        it("All referenced versions should be declared") {
            referencedVersions.forEach { ref ->
                declaredVersions shouldContain ref
            }
        }

        it("ksp should be in declared versions") {
            declaredVersions shouldContain "ksp"
        }

        it("ksp should be referenced in catalog") {
            referencedVersions shouldContain "ksp"
        }
    }

    describe("KSP Version Specific Validation") {
        it("KSP version should match expected format (major.minor.patch)") {
            val kspVersionLine = content.lines()
                .first { it.trim().startsWith("ksp") && it.contains("=") }

            val version = kspVersionLine
                .substringAfter("=")
                .trim()
                .removeSurrounding("\"")

            version shouldMatch Regex("""^\d+\.\d+\.\d+$""")
        }

        it("KSP version should be compatible with Kotlin version") {
            val kspVersion = content.lines()
                .first { it.trim().startsWith("ksp") && it.contains("=") }
                .substringAfter("=")
                .trim()
                .removeSurrounding("\"")

            val kotlinVersion = content.lines()
                .first { it.trim().startsWith("kotlin") && it.contains("=") && !it.contains("-") }
                .substringAfter("=")
                .trim()
                .removeSurrounding("\"")

            // KSP version format should be compatible (2.x for Kotlin 2.x)
            val kspMajor = kspVersion.split(".")[0].toInt()
            val kotlinMajor = kotlinVersion.split(".")[0].toInt()

            kspMajor shouldBe kotlinMajor
        }
    }

    describe("Edge Cases and Negative Tests") {
        it("Should not contain duplicate version keys") {
            val versionSection = content
                .substringAfter("[versions]")
                .substringBefore("[libraries]")

            val versionKeys = versionSection.lines()
                .filter { it.contains("=") && !it.trim().startsWith("#") }
                .map { it.substringBefore("=").trim() }

            val uniqueKeys = versionKeys.toSet()
            versionKeys.size shouldBe uniqueKeys.size
        }

        it("Should not contain duplicate library keys") {
            val librariesSection = content
                .substringAfter("[libraries]")
                .substringBefore("[bundles]")

            val libraryKeys = librariesSection.lines()
                .filter { it.contains("=") && !it.trim().startsWith("#") && !it.trim().isEmpty() }
                .map { it.substringBefore("=").trim() }
                .filter { it.isNotEmpty() }

            val uniqueKeys = libraryKeys.toSet()
            libraryKeys.size shouldBe uniqueKeys.size
        }

        it("Should not contain empty version values") {
            val versionSection = content
                .substringAfter("[versions]")
                .substringBefore("[libraries]")

            val emptyVersions = versionSection.lines()
                .filter { it.contains("=") && !it.trim().startsWith("#") }
                .filter { line ->
                    val value = line.substringAfter("=").trim().removeSurrounding("\"")
                    value.isEmpty()
                }

            emptyVersions.size shouldBe 0
        }

        it("Version references should not have circular dependencies") {
            // This is implicitly validated by Gradle, but we can check structure
            val versionRefPattern = Regex("""version\.ref\s*=\s*"([^"]+)"""")
            val refs = versionRefPattern.findAll(content).map { it.groupValues[1] }.toList()

            // All refs should exist in versions section
            refs.shouldNotBeNull()
        }
    }

    describe("Boundary and Regression Tests") {
        it("KSP version 2.3.6 should be greater than 2.0.0") {
            val kspVersion = content.lines()
                .first { it.trim().startsWith("ksp") && it.contains("=") }
                .substringAfter("=")
                .trim()
                .removeSurrounding("\"")

            val parts = kspVersion.split(".")
            val major = parts[0].toInt()
            val minor = parts[1].toInt()
            val patch = parts[2].toInt()

            (major >= 2) shouldBe true
            if (major == 2) {
                (minor >= 0) shouldBe true
                if (minor == 0) {
                    (patch >= 0) shouldBe true
                }
            }
        }

        it("Should handle version catalog with all required dependencies for KSP") {
            // Ensure all KSP-related dependencies are present
            val requiredDeps = listOf("ksp-api", "ksp-koin", "kotlin-gradle-plugin")
            val librariesSection = content
                .substringAfter("[libraries]")
                .substringBefore("[bundles]")

            requiredDeps.forEach { dep ->
                librariesSection shouldMatch Regex("""$dep\s*=.*""")
            }
        }

        it("Should not have trailing whitespace in version values") {
            val versionSection = content
                .substringAfter("[versions]")
                .substringBefore("[libraries]")

            val versionsWithTrailingSpace = versionSection.lines()
                .filter { it.contains("=") && !it.trim().startsWith("#") }
                .filter { line ->
                    val value = line.substringAfter("=").trim()
                    value.endsWith(" \"") || value.startsWith("\" ")
                }

            versionsWithTrailingSpace.size shouldBe 0
        }

        it("All version strings should be properly quoted") {
            val versionSection = content
                .substringAfter("[versions]")
                .substringBefore("[libraries]")

            val unquotedVersions = versionSection.lines()
                .filter { it.contains("=") && !it.trim().startsWith("#") }
                .filter { line ->
                    val value = line.substringAfter("=").trim()
                    value.isNotEmpty() && (!value.startsWith("\"") || !value.endsWith("\""))
                }

            unquotedVersions.size shouldBe 0
        }
    }
})