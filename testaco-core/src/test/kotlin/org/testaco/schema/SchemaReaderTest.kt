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
 */package org.testaco.schema

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.assertions.throwables.shouldThrow

class SchemaReaderTest : FunSpec({

    test("reads a valid schema file from classpath") {
        val schema = SchemaReader.fromClasspath("test-schemas/valid-schema.json")

        schema.schemas shouldBe listOf("public")
        schema.tables shouldContainKey "public.users"

        val users = schema.tables["public.users"]!!
        users.columns["id"]!!.type shouldBe "bigint"
        users.columns["id"]!!.nullable shouldBe false
        users.columns["email"]!!.nullable shouldBe true
        users.primaryKey shouldBe listOf("id")
    }

    test("reads foreign keys") {
        val schema = SchemaReader.fromClasspath("test-schemas/valid-schema.json")

        val orders = schema.tables["public.orders"]!!
        orders.foreignKeys.size shouldBe 1
        orders.foreignKeys[0].columns shouldBe listOf("user_id")
        orders.foreignKeys[0].references shouldBe "public.users"
        orders.foreignKeys[0].referencedColumns shouldBe listOf("id")
    }

    test("throws on missing schema file") {
        shouldThrow<IllegalArgumentException> {
            SchemaReader.fromClasspath("nonexistent.json")
        }
    }
})
