/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.testaco.gradle.functional

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

fun createTempDir(prefix: String): File {
    val dir = kotlin.io.path.createTempDirectory(prefix).toFile()
    dir.deleteOnExit()
    return dir
}

fun setupGradleProject(dir: File) {
    File(dir, "settings.gradle.kts").writeText("rootProject.name = \"test-project\"")
    File(dir, "build.gradle.kts").writeText("""
        plugins {
            id("org.testaco")
        }
    """.trimIndent())
}

fun runGradle(projectDir: File, vararg args: String): BuildResult {
    return GradleRunner.create().withProjectDir(projectDir).withArguments(*args).withPluginClasspath().build()
}
