@file:DependsOn("com.gianluz:danger-kotlin-android-lint-plugin:0.1.0")
@file:DependsOn("io.github.ackeecz:danger-kotlin-detekt:0.1.4")

import com.gianluz.dangerkotlin.androidlint.AndroidLint
import io.github.ackeecz.danger.detekt.DetektPlugin
import java.io.File
import systems.danger.kotlin.danger
import systems.danger.kotlin.onGitHub
import systems.danger.kotlin.register
import systems.danger.kotlin.warn

register plugin DetektPlugin

register plugin AndroidLint

danger(args) {
    val maxLines = 500
    if (git.linesOfCode > maxLines) {
        warn(":warning: affects more than $maxLines lines of code")
    }

    onGitHub {
        if ("WIP" in pullRequest.title) {
            warn(":construction: PR is marked with Work in Progress (WIP)")
        }
    }

    warnDetekt()

    val baselineRegex = Regex("([A-Za-z]+/)*detekt-baseline(-[A-Za-z]+)*\\.xml")
    val allModifiedFiles = git.modifiedFiles + git.createdFiles
    val baselineFiles = allModifiedFiles.filter { baselineRegex.containsMatchIn(it) }.distinct()

    onGitHub {
        baselineFiles.forEach { path ->
            val stats = utils.exec("git", listOf("diff", "--numstat", path))
            val additions = stats.substringBefore(' ').toInt()
            if (additions > 0) {
                val repoURL = pullRequest.base.repo.htmlURL
                val prSha = pullRequest.head.sha
                warn(":warning: Detekt warnings added to [$path]($repoURL/blob/$prSha/$path)")
            }
        }
    }

    AndroidLint.report("app/build/reports/lint-results-debug.xml")
    AndroidLint.report("app/konsist/build/reports/lint-results-debug.xml")

    val ianSrcRegex = Regex("IAN/([A-Za-z]+/)*src/(main|test|testFixtures)/")
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

    mainOnly.forEach { path ->
        warn(":test_tube: Source files at $path were modified without also modifying tests")
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
