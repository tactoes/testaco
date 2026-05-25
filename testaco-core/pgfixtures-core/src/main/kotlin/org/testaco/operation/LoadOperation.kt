package org.testaco.operation

import org.testaco.config.PgFixturesConfig
import org.testaco.config.LoadStrategy
import org.testaco.dataset.DataSet
import org.testaco.schema.Schema
import org.testaco.util.ForeignKeyResolver
import java.sql.Connection

object LoadOperation {

    fun execute(
        connection: Connection, dataSet: DataSet, schema: Schema,
        fkResolver: ForeignKeyResolver, config: PgFixturesConfig, strategy: LoadStrategy
    ) {
        val tables = dataSet.tables.keys.toList()
        val hasCycles = fkResolver.hasCycles(tables)

        connection.autoCommit = false
        try {
            if (hasCycles) connection.createStatement().execute("SET CONSTRAINTS ALL DEFERRED")

            if (strategy == LoadStrategy.CLEAN_INSERT) {
                for (table in fkResolver.truncateOrder(tables)) {
                    val (s, t) = table.split(".", limit = 2)
                    connection.createStatement().execute("TRUNCATE \"$s\".\"$t\" CASCADE")
                }
            }

            for (table in fkResolver.insertOrder(tables)) {
                val rows = dataSet.tables[table] ?: continue
                if (rows.isEmpty()) continue
                val (s, t) = table.split(".", limit = 2)

                for (row in rows) {
                    val filtered = row.filterKeys { !config.isColumnIgnored(table, it) }
                    val cols = filtered.keys.toList()
                    val placeholders = cols.joinToString(", ") { "?" }
                    val colNames = cols.joinToString(", ") { "\"$it\"" }
                    val sql = "INSERT INTO \"$s\".\"$t\" ($colNames) VALUES ($placeholders)"

                    connection.prepareStatement(sql).use { stmt ->
                        cols.forEachIndexed { i, col ->
                            val v = filtered[col]
                            if (v == null) stmt.setNull(i + 1, java.sql.Types.NULL) else stmt.setObject(i + 1, v)
                        }
                        stmt.executeUpdate()
                    }
                }
            }
            connection.commit()
        } catch (e: Exception) {
            connection.rollback(); throw e
        } finally {
            connection.autoCommit = true
        }
    }
}
