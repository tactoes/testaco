package org.testaco.dataset

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Paths

class DataSetWriterTest : FunSpec({

    test("writes dataset to JSON string") {
        val ds = DataSet(mapOf("users" to listOf(mapOf("id" to 1, "name" to "Alice"))))
        val json = DataSetWriter.toJson(ds)
        json shouldContain "\"users\""
        json shouldContain "\"Alice\""
    }

    test("writes dataset to file and reads back") {
        val ds = DataSet(mapOf("users" to listOf(mapOf("id" to 1, "name" to "Bob"))))
        val tempFile = Paths.get("build/test-output-${System.currentTimeMillis()}.json")
        try {
            DataSetWriter.toFile(ds, tempFile)
            val readBack = DataSetReader.fromString(Files.readString(tempFile))
            readBack.tables["users"]!![0]["name"] shouldBe "Bob"
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }
})
