package org.testaco.gradle.functional

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import java.io.File

class AddTableTaskTest : FunSpec({
    test("adds table to schema") {
        val dir = createTempDir("testaco-test"); setupGradleProject(dir)
        val res = File(dir, "src/test/resources"); res.mkdirs()
        File(res, "testaco-schema.json").writeText("""{"schemas":["public"],"tables":{}}""")

        runGradle(dir, "testacoAddTable", "--table=addresses", "--columns=id:bigint,street:text")
        File(res, "testaco-schema.json").readText() shouldContain "public.addresses"
    }
})
