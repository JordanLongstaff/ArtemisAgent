package artemis.agent.gradle

import info.solidsoft.gradle.pitest.PitestPluginExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import java.math.BigDecimal
import org.gradle.api.Project

private const val PITEST_VERSION = "1.20.1"
private const val JUNIT5_VERSION = "1.2.3"

private const val TIMEOUT_FACTOR = 10
private val MUTATORS =
    setOf(
        "STRONGER",
        "EXTENDED",
        "EXTREME",
        "INLINE_CONSTS",
        "REMOVE_CONDITIONALS",
        "REMOVE_INCREMENTS",
        "EXPERIMENTAL_MEMBER_VARIABLE",
        "EXPERIMENTAL_NAKED_RECEIVER",
    )

fun PitestPluginExtension.configure(rootPackage: String, threads: Int) {
    pitestVersion.set(PITEST_VERSION)
    junit5PluginVersion.set(JUNIT5_VERSION)
    verbose.set(true)
    targetClasses.set(listOf("$rootPackage.*"))
    this.threads.set(threads)
    timeoutFactor.set(BigDecimal(TIMEOUT_FACTOR))
    outputFormats.set(listOf("HTML", "CSV", "XML"))
    timestampedReports.set(false)
    setWithHistory(true)
    mutators.addAll(MUTATORS)
}

fun Project.dependsOnKonsist() {
    tasks
        .named { it.startsWith("assemble") }
        .forEach { task -> task.dependsOn("$path:konsist:test") }
}

fun DetektExtension.includeSourceSets(vararg sourceSets: String) {
    val allSourceSets = setOf("main", "test") + sourceSets
    val sourcePaths = allSourceSets.map { srcSet -> "src/$srcSet/kotlin" }
    source.setFrom(sourcePaths)
}
