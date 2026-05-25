/*
 * SPDX-License-Identifier: Apache-2.0
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
