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
        val db = PgFixtures(dataSource = ds(), schemaResourcePath = "test-schemas/valid-schema.json")
        db.load("test-datasets/public-users", "test-datasets/public-orders")
        db.assertMatches("test-datasets/expected-after-load")
    }

    test("dump returns in-memory dataset") {
        val db = PgFixtures(dataSource = ds(), schemaResourcePath = "test-schemas/valid-schema.json")
        db.load("test-datasets/public-users")
        val result = db.dump()
        result shouldContainKey "public.users"
    }

    test("assertMatches throws on mismatch") {
        val db = PgFixtures(dataSource = ds(), schemaResourcePath = "test-schemas/valid-schema.json")
        db.load("test-datasets/public-users")
        shouldThrow<DataSetMismatchException> {
            db.assertMatches("test-datasets/expected-mismatch")
        }
    }
})
