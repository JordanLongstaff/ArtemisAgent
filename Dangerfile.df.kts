@file:DependsOn("io.github.ackeecz:danger-kotlin-detekt:0.1.4")

import io.github.ackeecz.danger.detekt.DetektPlugin
import java.io.File
import systems.danger.kotlin.danger
import systems.danger.kotlin.linesOfCode
import systems.danger.kotlin.onGitHub
import systems.danger.kotlin.register
import systems.danger.kotlin.warn

register plugin DetektPlugin

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

    val detektReport = File("detekt.xml")
    if (detektReport.exists()) {
        DetektPlugin.parseAndReport(detektReport)
    } else {
        warn(":see_no_evil: No detekt report found")
    }

    val baselineRegex = Regex("([A-Za-z]+/)*detekt-baseline(-[A-Za-z]+)*\\.xml")
    val allModifiedFiles = git.modifiedFiles + git.createdFiles
    val baselineFiles = allModifiedFiles.filter { baselineRegex.containsMatchIn(it) }.distinct()

    onGitHub {
        val repoURL = pullRequest.base.repo.htmlURL
        val prSha = pullRequest.head.sha
        val baseSha = pullRequest.base.sha

        val whitespace = Regex("\\s+")

        baselineFiles.forEach { path ->
            val stats = utils.exec("git", listOf("diff", "--numstat", baseSha, prSha, path))
            if (stats.isBlank()) {
                warn("Could not fetch diff stats for $path\n**git diff --numstat $baseSha $prSha $path**")
                return@forEach
            }
            val additions = stats.split(whitespace).first().toInt()
            if (additions > 0) {
                warn(":warning: Detekt warnings added to [$path]($repoURL/blob/$prSha/$path)")
            }
        }
    }

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
