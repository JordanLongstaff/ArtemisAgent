import systems.danger.kotlin.rules.RuleResult
import systems.danger.kotlin.rules.rule
import systems.danger.kotlin.warn

rule(id = "main-no-tests") {
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

    mainOnly.forEach { path ->
        warn(":test_tube: Source files at $path were modified without also modifying tests")
    }

    RuleResult.Continue
}

