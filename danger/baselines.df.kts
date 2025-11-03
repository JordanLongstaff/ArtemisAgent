import systems.danger.kotlin.diffForFile
import systems.danger.kotlin.message
import systems.danger.kotlin.onGitHub
import systems.danger.kotlin.rules.RuleResult
import systems.danger.kotlin.rules.rule
import systems.danger.kotlin.warn

rule(id = "baselines") {
    val baselineRegex = Regex("([A-Za-z]+/)*detekt-baseline(-[A-Za-z]+)*\\.xml")
    val allModifiedFiles = git.modifiedFiles + git.createdFiles
    val baselineFiles = allModifiedFiles.filter { baselineRegex.containsMatchIn(it) }.distinct()

    message(
        """
        All modified files:
        ${allModifiedFiles.joinToString("\n") { "$it ${git.diffForFile(it).additions.size}" }}
    """
            .trimIndent()
    )

    onGitHub {
        val repoURL = pullRequest.base.repo.htmlURL
        val prSha = pullRequest.head.sha

        baselineFiles.forEach { path ->
            val additions = git.diffForFile(path).additions.size
            message("Detekt baseline file: $path with $additions additions")
            if (additions > 0) {
                warn(":warning: Detekt warnings added to [$path]($repoURL/blob/$prSha/$path)")
            }
        }
    }

    RuleResult.Continue
}
