import systems.danger.kotlin.onGitHub
import systems.danger.kotlin.rules.RuleResult
import systems.danger.kotlin.rules.rule
import systems.danger.kotlin.warn

rule(id = "wip") {
    onGitHub {
        if ("WIP" in pullRequest.title) {
            warn(":construction: PR is marked with Work in Progress (WIP)")
        }
    }
    RuleResult.Continue
}
