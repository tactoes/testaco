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

package org.testaco

import org.testaco.assertion.DataSetMismatchException
import org.testaco.config.TestacoConfig
import org.testaco.config.LoadStrategy
import org.testaco.dataset.DataSetReader
import org.testaco.operation.CompareOperation
import org.testaco.operation.DumpOperation
import org.testaco.operation.LoadOperation
import org.testaco.schema.SchemaReader
import org.testaco.schema.SchemaValidator
import org.testaco.util.ForeignKeyResolver
import org.testaco.util.PostgresIntrospector
import java.nio.file.Path
import javax.sql.DataSource

/**
 * Main Testaco API for database testing.
 *
 * Testaco validates that your database schema matches an expected schema, then helps you assert
 * that your application correctly loads and retrieves data.
 *
 * Basic usage:
 * ```kotlin
 * val dataSource = createDataSource() // PostgreSQL connection
 * val testaco = Testaco(dataSource)
 *
 * // Load test data
 * testaco.load("com/example/orders/setup")
 *
 * // Assert actual matches expected
 * testaco.assertMatches("com/example/orders/expected")
 *
 * // Or dump the actual database state for inspection
 * val actual = testaco.dump()
 * ```
 *
 * Configuration is loaded from `testaco-config.json` in the classpath. Schema is loaded from
 * `testaco-schema.json` by default (or specify a different path).
 *
 * Datasets are organized by test class/method: `src/test/resources/com/example/Test/testName/{setup,expected}.json`
 *
 * @param dataSource PostgreSQL datasource (JUnit 5 extension recommended: `@TestcontainersTest`)
 * @param config Optional Testaco configuration (defaults to loading from classpath)
 * @param schemaResourcePath Path to schema JSON file in classpath (default: "testaco-schema.json")
 * @throws IllegalArgumentException if schema doesn't match live database
 */
class Testaco(
    private val dataSource: DataSource,
    config: TestacoConfig? = null,
    schemaResourcePath: String = "testaco-schema.json"
) {
    private val config: TestacoConfig = config ?: TestacoConfig.fromClasspath()
    private val schema = SchemaReader.fromClasspath(schemaResourcePath)
    private val fkResolver = ForeignKeyResolver(schema)

    init {
        dataSource.connection.use { conn ->
            val live = PostgresIntrospector(conn).introspect(this.config.schemas)
            SchemaValidator.validateSchemaMatch(schema, live, this.config)
        }
    }

    /**
     * Loads dataset(s) from JSON files into the database, respecting foreign key order.
     *
     * @param resourcePaths One or more classpath paths to JSON dataset files (e.g., "com/example/orders/setup")
     * @param strategy How to handle existing data: TRUNCATE (default), INSERT, or UPSERT
     * @throws IllegalArgumentException if dataset doesn't match schema
     * @throws Exception if insert fails (constraint violations, FK errors)
     */
    fun load(vararg resourcePaths: String, strategy: LoadStrategy = config.loadStrategy) {
        val dataSet = DataSetReader.fromClasspath(*resourcePaths)
        SchemaValidator.validateDataSetAgainstSchema(dataSet, schema, config)
        dataSource.connection.use { conn ->
            LoadOperation.execute(conn, dataSet, schema, fkResolver, config, strategy)
        }
    }

    /**
     * Asserts that the database matches the expected dataset.
     *
     * On mismatch, dumps actual state to `build/test-results/testaco/{resourcePath}/actual.json`
     * for easy diff comparison.
     *
     * Supports flexible matchers in expected JSON:
     * - `~now` — current timestamp
     * - `~now±PT10S` — current timestamp ±10 seconds
     * - `~ignore` — skip column comparison
     * - `~regex:.*pattern.*` — regex pattern match
     *
     * @param resourcePath Classpath path to expected JSON dataset
     * @throws DataSetMismatchException if actual database doesn't match expected
     * @throws IllegalArgumentException if dataset doesn't match schema
     */
    fun assertMatches(resourcePath: String) {
        val expected = DataSetReader.fromClasspath(resourcePath)
        SchemaValidator.validateDataSetAgainstSchema(expected, schema, config)
        dataSource.connection.use { conn ->
            val result = CompareOperation.execute(conn, expected, schema, config)
            if (!result.matches) {
                val dumpPath = try {
                    val dir = resourcePath.substringBeforeLast('/', resourcePath)
                    val p = Path.of("build", "test-results", "testaco", dir, "actual.json")
                    p.parent?.toFile()?.mkdirs()
                    DumpOperation.toFile(conn, schema, config, p); p.toString()
                } catch (_: Exception) { null }
                throw DataSetMismatchException(result, dumpPath)
            }
        }
    }

    /**
     * Dumps the database state to a JSON file at the specified path.
     *
     * Useful for debugging or creating expected datasets.
     *
     * @param resourcePath Filesystem path where JSON will be written
     */
    fun dump(resourcePath: String) {
        dataSource.connection.use { conn ->
            DumpOperation.toFile(conn, schema, config, Path.of(resourcePath))
        }
    }

    /**
     * Dumps the database state to a Kotlin map structure.
     *
     * @return Map of table names to lists of row data
     */
    fun dump(): Map<String, List<Map<String, Any?>>> {
        dataSource.connection.use { conn ->
            return DumpOperation.toDataSet(conn, schema, config).tables
        }
    }
}
