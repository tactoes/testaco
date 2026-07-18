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

package org.testaco.schema

import org.testaco.config.TestacoConfig
import org.testaco.dataset.DataSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain

class SchemaValidatorTest : FunSpec({

    val schema = Schema(
        schemas = listOf("public"),
        tables = mapOf(
            "public.users" to TableDef(
                columns = mapOf(
                    "id" to ColumnDef("bigint", false),
                    "name" to ColumnDef("text", false),
                    "email" to ColumnDef("text", true)
                ),
                primaryKey = listOf("id")
            )
        )
    )
    val config = TestacoConfig()

    test("identical schemas pass validation") {
        SchemaValidator.validateSchemaMatch(schema, schema.copy(), config)
    }

    test("missing table in live DB is reported") {
        val liveSchema = Schema(schemas = listOf("public"), tables = emptyMap())
        val ex = shouldThrow<SchemaValidationException> {
            SchemaValidator.validateSchemaMatch(schema, liveSchema, config)
        }
        ex.message shouldContain "Missing tables"
    }

    test("column type mismatch is reported") {
        val liveSchema = Schema(
            schemas = listOf("public"),
            tables = mapOf(
                "public.users" to TableDef(
                    columns = mapOf(
                        "id" to ColumnDef("integer", false),
                        "name" to ColumnDef("text", false),
                        "email" to ColumnDef("text", true)
                    ),
                    primaryKey = listOf("id")
                )
            )
        )
        val ex = shouldThrow<SchemaValidationException> {
            SchemaValidator.validateSchemaMatch(schema, liveSchema, config)
        }
        ex.message shouldContain "bigint"
    }

    test("dataset referencing unknown column fails") {
        val dataset = DataSet(mapOf("public.users" to listOf(mapOf("id" to 1, "bogus" to "x"))))
        val ex = shouldThrow<DataSetValidationException> {
            SchemaValidator.validateDataSetAgainstSchema(dataset, schema, config)
        }
        ex.message shouldContain "bogus"
    }

    test("dataset referencing unknown table fails") {
        val dataset = DataSet(mapOf("public.nope" to listOf(mapOf("id" to 1))))
        val ex = shouldThrow<DataSetValidationException> {
            SchemaValidator.validateDataSetAgainstSchema(dataset, schema, config)
        }
        ex.message shouldContain "nope"
    }

    test("ignored columns are excluded from validation") {
        val configWithIgnored = TestacoConfig(ignoredColumns = mapOf("public.users" to listOf("email")))
        val liveSchema = Schema(
            schemas = listOf("public"),
            tables = mapOf(
                "public.users" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false), "name" to ColumnDef("text", false)),
                    primaryKey = listOf("id")
                )
            )
        )
        SchemaValidator.validateSchemaMatch(schema, liveSchema, configWithIgnored)
    }
})
