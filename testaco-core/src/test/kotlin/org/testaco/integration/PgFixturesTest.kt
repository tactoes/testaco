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

import org.testaco.Testaco
import org.testaco.assertion.DataSetMismatchException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.assertions.throwables.shouldThrow
import org.testcontainers.containers.PostgreSQLContainer
import org.postgresql.ds.PGSimpleDataSource

class TestacoTest : FunSpec({

    val postgres = PostgreSQLContainer("postgres:16-alpine")

    beforeSpec {
        postgres.start()
        PGSimpleDataSource().apply {
            setURL(postgres.jdbcUrl); user = postgres.username; password = postgres.password
        }.connection.use { conn ->
            conn.createStatement().execute("""
                CREATE TABLE users (id BIGINT PRIMARY KEY, name TEXT NOT NULL, email TEXT);
                CREATE TABLE orders (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), amount NUMERIC NOT NULL);
            """)
        }
    }
    afterSpec { postgres.stop() }

    fun ds() = PGSimpleDataSource().apply {
        setURL(postgres.jdbcUrl); user = postgres.username; password = postgres.password
    }

    beforeTest {
        ds().connection.use { conn ->
            conn.createStatement().execute("TRUNCATE TABLE orders, users CASCADE")
        }
    }

    test("load and assertMatches round-trip") {
        val db = Testaco(dataSource = ds(), schemaResourcePath = "test-schemas/valid-schema.json")
        db.load("test-datasets/public-users", "test-datasets/public-orders")
        db.assertMatches("test-datasets/expected-after-load")
    }

    test("dump returns in-memory dataset") {
        val db = Testaco(dataSource = ds(), schemaResourcePath = "test-schemas/valid-schema.json")
        db.load("test-datasets/public-users")
        val result = db.dump()
        result shouldContainKey "public.users"
    }

    test("assertMatches throws on mismatch") {
        val db = Testaco(dataSource = ds(), schemaResourcePath = "test-schemas/valid-schema.json")
        db.load("test-datasets/public-users")
        shouldThrow<DataSetMismatchException> {
            db.assertMatches("test-datasets/expected-mismatch")
        }
    }
})
