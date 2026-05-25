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
 */package org.testaco.dataset

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.assertions.throwables.shouldThrow

class DataSetReaderTest : FunSpec({

    test("reads a dataset from classpath") {
        val ds = DataSetReader.fromClasspath("test-datasets/users")

        ds.tables.keys shouldBe setOf("users")
        ds.tables["users"]!! shouldHaveSize 2
        ds.tables["users"]!![0]["name"] shouldBe "Alice"
    }

    test("merges multiple datasets") {
        val ds = DataSetReader.fromClasspath(
            "test-datasets/users",
            "test-datasets/orders"
        )

        ds.tables.keys shouldBe setOf("users", "orders")
        ds.tables["users"]!! shouldHaveSize 2
        ds.tables["orders"]!! shouldHaveSize 1
    }

    test("merging appends rows for same table") {
        val ds = DataSetReader.fromClasspath(
            "test-datasets/users",
            "test-datasets/more-users"
        )

        ds.tables["users"]!! shouldHaveSize 3
    }

    test("throws on missing dataset file") {
        shouldThrow<IllegalArgumentException> {
            DataSetReader.fromClasspath("nonexistent")
        }
    }
})
