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
