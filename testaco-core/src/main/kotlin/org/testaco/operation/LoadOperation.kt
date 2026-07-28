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

package org.testaco.operation

import org.slf4j.LoggerFactory
import org.testaco.config.TestacoConfig
import org.testaco.config.LoadStrategy
import org.testaco.dataset.DataSet
import org.testaco.schema.Schema
import org.testaco.util.ForeignKeyResolver
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

object LoadOperation {
    private val logger = LoggerFactory.getLogger(LoadOperation::class.java)

    fun execute(
        connection: Connection, dataSet: DataSet, schema: Schema,
        fkResolver: ForeignKeyResolver, config: TestacoConfig, strategy: LoadStrategy
    ) {
        val tables = dataSet.tables.keys.toList()
        val hasCycles = fkResolver.hasCycles(tables)

        logger.debug("Starting load operation for {} tables with strategy {}", tables.size, strategy)
        if (hasCycles) logger.debug("Detected circular foreign key dependencies, deferring constraints")

        connection.autoCommit = false
        try {
            if (hasCycles) connection.createStatement().execute("SET CONSTRAINTS ALL DEFERRED")

            if (strategy == LoadStrategy.CLEAN_INSERT) {
                logger.debug("Truncating tables in reverse FK order")
                for (table in fkResolver.truncateOrder(tables)) {
                    val (s, t) = table.split(".", limit = 2)
                    connection.createStatement().execute("TRUNCATE \"$s\".\"$t\" CASCADE")
                    logger.debug("Truncated table {}", table)
                }
            }

            for (table in fkResolver.insertOrder(tables)) {
                val rows = dataSet.tables[table] ?: continue
                if (rows.isEmpty()) {
                    logger.debug("Skipping empty table {}", table)
                    continue
                }
                val (s, t) = table.split(".", limit = 2)
                logger.debug("Inserting {} rows into table {}", rows.size, table)

                for (row in rows) {
                    val filtered = row.filterKeys { !config.isColumnIgnored(table, it) }
                    val cols = filtered.keys.toList()
                    val placeholders = cols.joinToString(", ") { "?" }
                    val colNames = cols.joinToString(", ") { "\"$it\"" }
                    val sql = "INSERT INTO \"$s\".\"$t\" ($colNames) VALUES ($placeholders)"

                    connection.prepareStatement(sql).use { stmt ->
                        cols.forEachIndexed { i, col ->
                            val v = filtered[col]
                            bindParameter(stmt, i + 1, v)
                        }
                        stmt.executeUpdate()
                    }
                }
            }
            logger.debug("Load operation completed successfully")
            connection.commit()
        } catch (e: Exception) {
            logger.error("Load operation failed, rolling back", e)
            connection.rollback(); throw e
        } finally {
            connection.autoCommit = true
        }
    }

    private fun bindParameter(stmt: PreparedStatement, index: Int, value: Any?) {
        when (value) {
            null -> stmt.setNull(index, java.sql.Types.NULL)
            is String -> stmt.setString(index, value)
            is Long -> stmt.setLong(index, value)
            is Int -> stmt.setInt(index, value)
            is Short -> stmt.setShort(index, value)
            is Byte -> stmt.setByte(index, value)
            is Boolean -> stmt.setBoolean(index, value)
            is Double -> stmt.setDouble(index, value)
            is Float -> stmt.setFloat(index, value)
            is BigDecimal -> stmt.setBigDecimal(index, value)
            is BigInteger -> stmt.setBigDecimal(index, value.toBigDecimal())
            is java.sql.Date -> stmt.setDate(index, value)
            is java.sql.Time -> stmt.setTime(index, value)
            is Timestamp -> stmt.setTimestamp(index, value)
            is LocalDate -> stmt.setDate(index, java.sql.Date.valueOf(value))
            is LocalDateTime -> stmt.setTimestamp(index, Timestamp.valueOf(value))
            is Instant -> stmt.setTimestamp(index, Timestamp.from(value))
            else -> stmt.setObject(index, value)
        }
    }
}
