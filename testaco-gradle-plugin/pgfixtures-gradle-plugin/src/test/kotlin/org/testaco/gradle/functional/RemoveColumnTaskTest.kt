package org.testaco.gradle.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class RemoveColumnTaskTest : FunSpec({
    test("removes column from schema and datasets") {
        val dir = createTempDir("testaco-test"); setupGradleProject(dir)
        val res = File(dir, "src/test/resources"); res.mkdirs()
        File(res, "testaco-schema.json").writeText("""{"schemas":["public"],"tables":{"public.users":{"columns":{"id":{"type":"bigint","nullable":false},"phone":{"type":"text","nullable":true}},"primaryKey":["id"]}}}""")
        val ds = File(res, "test/setup.json"); ds.parentFile.mkdirs()
        ds.writeText("""{"users":[{"id":1,"phone":"555-1234"}]}""")

        runGradle(dir, "testacoRemoveColumn", "--table=users", "--column=phone")
        File(res, "testaco-schema.json").readText() shouldNotContain "\"phone\""
        ds.readText() shouldNotContain "\"phone\""
    }
})
