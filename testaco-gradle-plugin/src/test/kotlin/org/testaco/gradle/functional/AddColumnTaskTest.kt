/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.testaco.gradle.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import java.io.File

class AddColumnTaskTest : FunSpec({
    test("adds column to schema and dataset files") {
        val dir = createTempDir("testaco-test"); setupGradleProject(dir)
        val res = File(dir, "src/test/resources"); res.mkdirs()
        File(res, "testaco-schema.json").writeText("""{"schemas":["public"],"tables":{"public.users":{"columns":{"id":{"type":"bigint","nullable":false}},"primaryKey":["id"]}}}""")
        val ds = File(res, "test/setup.json"); ds.parentFile.mkdirs()
        ds.writeText("""{"users":[{"id":1}]}""")

        val result = runGradle(dir, "testacoAddColumn", "--table=users", "--column=email", "--type=text")
        result.output shouldContain "Added column"
        File(res, "testaco-schema.json").readText() shouldContain "\"email\""
    }

    test("adds column with default value for non-null") {
        val dir = createTempDir("testaco-test"); setupGradleProject(dir)
        val res = File(dir, "src/test/resources"); res.mkdirs()
        File(res, "testaco-schema.json").writeText("""{"schemas":["public"],"tables":{"public.users":{"columns":{"id":{"type":"bigint","nullable":false}},"primaryKey":["id"]}}}""")
        val ds = File(res, "test/setup.json"); ds.parentFile.mkdirs()
        ds.writeText("""{"users":[{"id":1}]}""")

        runGradle(dir, "testacoAddColumn", "--table=users", "--column=status", "--type=text", "--nullable=false", "--default=active")
        ds.readText() shouldContain "\"active\""
    }
})
