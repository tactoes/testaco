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
