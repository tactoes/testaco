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

import org.testaco.util.PostgresIntrospector
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.maps.shouldContainKey
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

class PostgresIntrospectorTest : FunSpec({

    val postgres = PostgreSQLContainer("postgres:16-alpine")

    beforeSpec {
        postgres.start()
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().execute("""
                CREATE TABLE users (
                    id BIGINT PRIMARY KEY,
                    name TEXT NOT NULL,
                    email TEXT
                );
                CREATE TABLE orders (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT NOT NULL REFERENCES users(id),
                    amount NUMERIC NOT NULL
                );
            """)
        }
    }

    afterSpec { postgres.stop() }

    test("introspects tables and columns") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val introspector = PostgresIntrospector(conn)
            val schema = introspector.introspect(listOf("public"))

            schema.tables shouldContainKey "public.users"
            schema.tables["public.users"]!!.columns["id"]!!.type shouldBe "bigint"
            schema.tables["public.users"]!!.columns["id"]!!.nullable shouldBe false
            schema.tables["public.users"]!!.columns["email"]!!.nullable shouldBe true
        }
    }

    test("introspects primary keys") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val introspector = PostgresIntrospector(conn)
            val schema = introspector.introspect(listOf("public"))

            schema.tables["public.users"]!!.primaryKey shouldBe listOf("id")
        }
    }

    test("introspects foreign keys") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val introspector = PostgresIntrospector(conn)
            val schema = introspector.introspect(listOf("public"))

            val orderFks = schema.tables["public.orders"]!!.foreignKeys
            orderFks.size shouldBe 1
            orderFks[0].columns shouldBe listOf("user_id")
            orderFks[0].references shouldBe "public.users"
        }
    }
})
