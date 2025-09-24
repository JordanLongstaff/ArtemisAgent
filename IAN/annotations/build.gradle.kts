import artemis.agent.gradle.dependsOnKonsist

plugins { id("ian-library") }

dependsOnKonsist()

dependencies { api(libs.kotlin.stdlib) }
