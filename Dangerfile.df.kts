@file:DependsOn("com.gianluz:danger-kotlin-android-lint-plugin:0.1.0")
@file:DependsOn("io.github.ackeecz:danger-kotlin-detekt:0.1.4")

import com.gianluz.dangerkotlin.androidlint.AndroidLint
import io.github.ackeecz.danger.detekt.DetektPlugin
import java.io.File
import systems.danger.kotlin.danger
import systems.danger.kotlin.linesOfCode
import systems.danger.kotlin.models.github.GitHub
import systems.danger.kotlin.onGitHub
import systems.danger.kotlin.register
import systems.danger.kotlin.warn

register plugin DetektPlugin

register plugin AndroidLint

val MAX_LINES = 500

danger(args) {
    warnDetekt()

    AndroidLint.report("app/build/reports/lint-results-debug.xml")
    AndroidLint.report("app/konsist/build/reports/lint-results-debug.xml")

    if (git.linesOfCode > MAX_LINES) {
        warn(":warning: affects more than $MAX_LINES lines of code")
    }

    val ianSrcRegex = Regex("IAN/([A-Za-z]+/)*src/(main|test|testFixtures)/")
    val allModifiedFiles = git.modifiedFiles + git.createdFiles
    val ianSrcChanges =
        allModifiedFiles
            .filter { ianSrcRegex.containsMatchIn(it) }
            .map { it.substringBeforeLast('/') }
            .distinct()
    val (ianSrcTest, ianSrcMain) = ianSrcChanges.partition { it.contains("src/test") }

    val mainOnly =
        ianSrcMain.filter { path ->
            val testRegex = Regex(path.replace("main", "test(Fixtures)?"))
            ianSrcTest.any { testRegex.containsMatchIn(it) }
        }

    onGitHub {
        warnWorkInProgress()

        mainOnly.forEach { path ->
            warn(":test_tube: Source files at $path were modified without also modifying tests")
        }
    }
}

fun warnDetekt() {
    val detektReport = File("build/reports/detekt/detekt.sarif")
    if (!detektReport.exists()) {
        warn(":see_no_evil: No detekt report found")
        return
    }
    DetektPlugin.parseAndReport(detektReport)
}

fun GitHub.warnWorkInProgress() {
    if ("WIP" in pullRequest.title) {
        warn(":construction: PR is marked with Work in Progress (WIP)")
    }
}
