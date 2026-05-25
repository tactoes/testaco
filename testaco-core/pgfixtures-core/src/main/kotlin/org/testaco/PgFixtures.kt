package org.testaco

import org.testaco.assertion.DataSetMismatchException
import org.testaco.config.PgFixturesConfig
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

class PgFixtures(
    private val dataSource: DataSource,
    config: PgFixturesConfig? = null,
    schemaResourcePath: String = "testaco-schema.json"
) {
    private val config: PgFixturesConfig = config ?: PgFixturesConfig.fromClasspath()
    private val schema = SchemaReader.fromClasspath(schemaResourcePath)
    private val fkResolver = ForeignKeyResolver(schema)

    init {
        dataSource.connection.use { conn ->
            val live = PostgresIntrospector(conn).introspect(this.config.schemas)
            SchemaValidator.validateSchemaMatch(schema, live, this.config)
        }
    }

    fun load(vararg resourcePaths: String, strategy: LoadStrategy = config.loadStrategy) {
        val dataSet = DataSetReader.fromClasspath(*resourcePaths)
        SchemaValidator.validateDataSetAgainstSchema(dataSet, schema, config)
        dataSource.connection.use { conn ->
            LoadOperation.execute(conn, dataSet, schema, fkResolver, config, strategy)
        }
    }

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

    fun dump(resourcePath: String) {
        dataSource.connection.use { conn ->
            DumpOperation.toFile(conn, schema, config, Path.of(resourcePath))
        }
    }

    fun dump(): Map<String, List<Map<String, Any?>>> {
        dataSource.connection.use { conn ->
            return DumpOperation.toDataSet(conn, schema, config).tables
        }
    }
}
