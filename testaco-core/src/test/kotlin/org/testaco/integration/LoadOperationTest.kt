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

package org.testaco.integration

import org.testaco.config.TestacoConfig
import org.testaco.config.LoadStrategy
import org.testaco.dataset.DataSet
import org.testaco.operation.LoadOperation
import org.testaco.schema.*
import org.testaco.util.ForeignKeyResolver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

class LoadOperationTest : FunSpec({

    val postgres = PostgreSQLContainer("postgres:16-alpine")

    beforeSpec {
        postgres.start()
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().execute("""
                CREATE TABLE users (id BIGINT PRIMARY KEY, name TEXT NOT NULL, email TEXT);
                CREATE TABLE orders (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), amount NUMERIC NOT NULL);
            """)
        }
    }
    afterSpec { postgres.stop() }

    val schema = Schema(
        schemas = listOf("public"),
        tables = mapOf(
            "public.users" to TableDef(
                columns = mapOf("id" to ColumnDef("bigint", false), "name" to ColumnDef("text", false), "email" to ColumnDef("text", true)),
                primaryKey = listOf("id")
            ),
            "public.orders" to TableDef(
                columns = mapOf("id" to ColumnDef("bigint", false), "user_id" to ColumnDef("bigint", false), "amount" to ColumnDef("numeric", false)),
                primaryKey = listOf("id"),
                foreignKeys = listOf(ForeignKeyDef(listOf("user_id"), "public.users", listOf("id")))
            )
        )
    )

    test("CLEAN_INSERT truncates and inserts") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().execute("INSERT INTO users (id, name) VALUES (99, 'OldUser') ON CONFLICT DO NOTHING")
            val dataset = DataSet(mapOf(
                "public.users" to listOf(mapOf("id" to 1L, "name" to "Alice", "email" to "alice@example.com")),
                "public.orders" to listOf(mapOf("id" to 10L, "user_id" to 1L, "amount" to 99.99))
            ))
            LoadOperation.execute(conn, dataset, schema, ForeignKeyResolver(schema), TestacoConfig(), LoadStrategy.CLEAN_INSERT)

            val rs = conn.createStatement().executeQuery("SELECT count(*) FROM users")
            rs.next(); rs.getInt(1) shouldBe 1
        }
    }

    test("INSERT adds without truncating") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().execute("TRUNCATE orders, users CASCADE")
            conn.createStatement().execute("INSERT INTO users (id, name) VALUES (50, 'Existing')")
            val dataset = DataSet(mapOf("public.users" to listOf(mapOf("id" to 1L, "name" to "Alice"))))
            LoadOperation.execute(conn, dataset, schema, ForeignKeyResolver(schema), TestacoConfig(), LoadStrategy.INSERT)

            val rs = conn.createStatement().executeQuery("SELECT count(*) FROM users")
            rs.next(); rs.getInt(1) shouldBe 2
        }
    }
})
