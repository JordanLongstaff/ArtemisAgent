import systems.danger.kotlin.linesOfCode
import systems.danger.kotlin.rules.RuleResult
import systems.danger.kotlin.rules.rule
import systems.danger.kotlin.warn

rule(id = "max-lines") {
    val maxLines = 500
    if (git.linesOfCode > maxLines) {
        warn(":warning: affects more than $maxLines lines of code")
    }
    RuleResult.Continue
}
