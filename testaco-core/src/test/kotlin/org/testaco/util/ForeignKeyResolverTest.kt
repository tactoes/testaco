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

package org.testaco.util

import org.testaco.schema.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainInOrder

class ForeignKeyResolverTest : FunSpec({

    test("resolves simple FK dependency order") {
        val schema = Schema(
            schemas = listOf("public"),
            tables = mapOf(
                "public.orders" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false), "user_id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id"),
                    foreignKeys = listOf(ForeignKeyDef(listOf("user_id"), "public.users", listOf("id")))
                ),
                "public.users" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id")
                )
            )
        )

        val resolver = ForeignKeyResolver(schema)
        val insertOrder = resolver.insertOrder(listOf("public.users", "public.orders"))

        insertOrder shouldContainInOrder listOf("public.users", "public.orders")
    }

    test("detects cycles") {
        val schema = Schema(
            schemas = listOf("public"),
            tables = mapOf(
                "public.a" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false), "b_id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id"),
                    foreignKeys = listOf(ForeignKeyDef(listOf("b_id"), "public.b", listOf("id")))
                ),
                "public.b" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false), "a_id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id"),
                    foreignKeys = listOf(ForeignKeyDef(listOf("a_id"), "public.a", listOf("id")))
                )
            )
        )

        val resolver = ForeignKeyResolver(schema)
        resolver.hasCycles(listOf("public.a", "public.b")) shouldBe true
    }

    test("truncate order is reverse of insert order") {
        val schema = Schema(
            schemas = listOf("public"),
            tables = mapOf(
                "public.orders" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false), "user_id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id"),
                    foreignKeys = listOf(ForeignKeyDef(listOf("user_id"), "public.users", listOf("id")))
                ),
                "public.users" to TableDef(
                    columns = mapOf("id" to ColumnDef("bigint", false)),
                    primaryKey = listOf("id")
                )
            )
        )

        val resolver = ForeignKeyResolver(schema)
        val truncateOrder = resolver.truncateOrder(listOf("public.users", "public.orders"))

        truncateOrder shouldContainInOrder listOf("public.orders", "public.users")
    }
})
