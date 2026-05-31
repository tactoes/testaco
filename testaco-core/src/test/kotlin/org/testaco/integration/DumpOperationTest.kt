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
import org.testaco.operation.DumpOperation
import org.testaco.schema.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.maps.shouldContainKey
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

class DumpOperationTest : FunSpec({

    val postgres = PostgreSQLContainer("postgres:16-alpine")

    beforeSpec {
        postgres.start()
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().execute("CREATE TABLE users (id BIGINT PRIMARY KEY, name TEXT NOT NULL);")
            conn.createStatement().execute("INSERT INTO users VALUES (1, 'Alice')")
            conn.createStatement().execute("INSERT INTO users VALUES (2, 'Bob')")
        }
    }
    afterSpec { postgres.stop() }

    val schema = Schema(
        schemas = listOf("public"),
        tables = mapOf("public.users" to TableDef(
            columns = mapOf("id" to ColumnDef("bigint", false), "name" to ColumnDef("text", false)),
            primaryKey = listOf("id")
        ))
    )

    test("dumps database to in-memory dataset") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val result = DumpOperation.toDataSet(conn, schema, TestacoConfig())
            result.tables shouldContainKey "public.users"
            result.tables["public.users"]!!.size shouldBe 2
        }
    }
})
