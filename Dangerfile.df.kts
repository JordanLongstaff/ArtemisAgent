@file:DependsOn("io.github.ackeecz:danger-kotlin-detekt:0.1.4")

import io.github.ackeecz.danger.detekt.DetektPlugin
import java.io.File
import systems.danger.kotlin.danger
import systems.danger.kotlin.linesOfCode
import systems.danger.kotlin.models.github.GitHub
import systems.danger.kotlin.onGitHub
import systems.danger.kotlin.register
import systems.danger.kotlin.warn

register plugin DetektPlugin

danger(args) {
    warnDetekt()

    val maxLines = 500
    if (git.linesOfCode > maxLines) {
        warn(":warning: affects more than $maxLines lines of code")
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
    val detektReport = File("detekt.xml")
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
