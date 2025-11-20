package artemis.agent.gradle

import com.arcmutate.gradle.github.GithubExtension
import com.arcmutate.gradle.github.PitestGithubPlugin
import info.solidsoft.gradle.pitest.PitestPluginExtension
import java.math.BigDecimal
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

private const val PITEST_VERSION = "1.22.0"
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

fun Project.configurePitest(
    rootPackage: String,
    threads: Int,
    pitestExtraConfig: PitestPluginExtension.() -> Unit = {},
) {
    apply<PitestGithubPlugin>()

    configure<PitestPluginExtension> {
        pitestVersion.set(PITEST_VERSION)
        junit5PluginVersion.set(JUNIT5_VERSION)
        verbose.set(true)
        targetClasses.set(listOf("$rootPackage.*"))
        this.threads.set(threads)
        timeoutFactor.set(BigDecimal(TIMEOUT_FACTOR))
        outputFormats.set(listOf("HTML", "CSV", "XML", "gitci"))
        features.set(listOf("+GIT(from[HEAD~1])"))
        timestampedReports.set(false)
        setWithHistory(true)
        mutators.addAll(MUTATORS)
        pitestExtraConfig()
    }

    configure<GithubExtension> {
        mutantEmoji.set(":radiation:")
        killedEmoji.set(":skull:")
    }
}

fun Project.configureTests(maxMemoryGb: Int = 2) {
    tasks.withType<Test>().configureEach {
        jvmArgs(
            "-Xmx${maxMemoryGb}g",
            "-Xms1g",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:+UseParallelGC",
        )
        useJUnitPlatform()
    }
}

fun Project.dependsOnKonsist() {
    tasks
        .named { it.startsWith("assemble") }
        .forEach { task -> task.dependsOn("$path:konsist:test") }
}
