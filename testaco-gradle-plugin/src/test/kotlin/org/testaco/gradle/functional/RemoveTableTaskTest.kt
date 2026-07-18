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
 */

package org.testaco.gradle.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class RemoveTableTaskTest : FunSpec({
    test("removes table from schema and datasets") {
        val dir = createTempDir("testaco-test"); setupGradleProject(dir)
        val res = File(dir, "src/test/resources"); res.mkdirs()
        File(res, "testaco-schema.json").writeText("""{"schemas":["public"],"tables":{"public.addresses":{"columns":{"id":{"type":"bigint","nullable":false}},"primaryKey":["id"]}}}""")
        val ds = File(res, "test/setup.json"); ds.parentFile.mkdirs()
        ds.writeText("""{"addresses":[{"id":1}]}""")

        runGradle(dir, "testacoRemoveTable", "--table=addresses")
        File(res, "testaco-schema.json").readText() shouldNotContain "addresses"
        ds.readText() shouldNotContain "addresses"
    }
})
