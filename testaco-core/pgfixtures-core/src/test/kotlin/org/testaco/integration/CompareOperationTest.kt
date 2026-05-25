package org.testaco.integration

import org.testaco.config.PgFixturesConfig
import org.testaco.dataset.DataSet
import org.testaco.operation.CompareOperation
import org.testaco.schema.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

class CompareOperationTest : FunSpec({

    val postgres = PostgreSQLContainer("postgres:16-alpine")

    beforeSpec {
        postgres.start()
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            conn.createStatement().execute("CREATE TABLE users (id BIGINT PRIMARY KEY, name TEXT NOT NULL, email TEXT);")
            conn.createStatement().execute("INSERT INTO users VALUES (1, 'Alice', 'alice@example.com')")
            conn.createStatement().execute("INSERT INTO users VALUES (2, 'Bob', null)")
        }
    }
    afterSpec { postgres.stop() }

    val schema = Schema(
        schemas = listOf("public"),
        tables = mapOf("public.users" to TableDef(
            columns = mapOf("id" to ColumnDef("bigint", false), "name" to ColumnDef("text", false), "email" to ColumnDef("text", true)),
            primaryKey = listOf("id")
        ))
    )

    test("matching data returns success") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val expected = DataSet(mapOf("public.users" to listOf(
                mapOf("id" to 1L, "name" to "Alice", "email" to "alice@example.com"),
                mapOf("id" to 2L, "name" to "Bob", "email" to null)
            )))
            CompareOperation.execute(conn, expected, schema, PgFixturesConfig()).matches shouldBe true
        }
    }

    test("mismatched data returns failure") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val expected = DataSet(mapOf("public.users" to listOf(
                mapOf("id" to 1L, "name" to "WRONG", "email" to "alice@example.com"),
                mapOf("id" to 2L, "name" to "Bob", "email" to null)
            )))
            val result = CompareOperation.execute(conn, expected, schema, PgFixturesConfig())
            result.matches shouldBe false
            result.tableDiffs["public.users"]!!.columnDiffs.isNotEmpty() shouldBe true
        }
    }

    test("~ignore matcher skips column") {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { conn ->
            val expected = DataSet(mapOf("public.users" to listOf(
                mapOf("id" to 1L, "name" to "Alice", "email" to "~ignore"),
                mapOf("id" to 2L, "name" to "Bob", "email" to "~ignore")
            )))
            CompareOperation.execute(conn, expected, schema, PgFixturesConfig()).matches shouldBe true
        }
    }
})
