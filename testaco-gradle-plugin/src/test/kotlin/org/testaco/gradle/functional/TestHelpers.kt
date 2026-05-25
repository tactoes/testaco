/*
 * Copyright 2026 Geir Hedemark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */package org.testaco.gradle.functional

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
