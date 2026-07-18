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
import org.testaco.assertion.*
import org.testaco.config.TestacoConfig
import org.testaco.dataset.DataSet
import org.testaco.schema.Schema
import java.sql.Connection

object CompareOperation {
    private val logger = LoggerFactory.getLogger(CompareOperation::class.java)

    private fun validateAndQuoteColumnNames(columnNames: List<String>): String {
        // Validate that column names only contain safe characters (alphanumeric, underscore)
        // This prevents SQL injection when building ORDER BY clauses
        require(columnNames.all { it.matches(Regex("[a-zA-Z0-9_]+")) }) {
            "Column names must be alphanumeric with underscores only"
        }
        return if (columnNames.isEmpty()) "1" else columnNames.joinToString(", ") { "\"$it\"" }
    }

    fun execute(connection: Connection, expected: DataSet, schema: Schema, config: TestacoConfig): ComparisonResult {
        logger.debug("Starting comparison of {} tables", expected.tables.size)
        val tableDiffs = mutableMapOf<String, TableDiff>()
        var allMatch = true

        for ((tableName, expectedRows) in expected.tables) {
            val tableDef = schema.tables[tableName] ?: continue
            val pk = tableDef.primaryKey
            val (s, t) = tableName.split(".", limit = 2)
            logger.debug("Comparing table {} ({} expected rows)", tableName, expectedRows.size)

            val orderBy = validateAndQuoteColumnNames(pk)

            val actualRows = mutableListOf<Map<String, Any?>>()
            connection.createStatement().use { stmt ->
                stmt.executeQuery("SELECT * FROM \"$s\".\"$t\" ORDER BY $orderBy").use { rs ->
                    val meta = rs.metaData
                    while (rs.next()) {
                        val row = mutableMapOf<String, Any?>()
                        for (i in 1..meta.columnCount) {
                            val col = meta.getColumnName(i)
                            if (!config.isColumnIgnored(tableName, col)) row[col] = rs.getObject(i)
                        }
                        actualRows.add(row)
                    }
                }
            }
            logger.debug("Retrieved {} actual rows from table {}", actualRows.size, tableName)

            val filtered = expectedRows.map { it.filterKeys { k -> !config.isColumnIgnored(tableName, k) } }
            val diff = compareRows(filtered, actualRows, pk, config)
            if (diff != null) { 
                logger.debug("Table {} has differences: {} missing, {} extra, {} column mismatches", 
                    tableName, diff.missingRows.size, diff.extraRows.size, diff.columnDiffs.size)
                tableDiffs[tableName] = diff; allMatch = false 
            } else {
                logger.debug("Table {} matches perfectly", tableName)
            }
        }

        return if (allMatch) {
            logger.debug("All tables match")
            ComparisonResult.match()
        } else {
            logger.debug("Comparison found mismatches in {} table(s)", tableDiffs.size)
            ComparisonResult(false, tableDiffs)
        }
    }

    private fun compareRows(expected: List<Map<String, Any?>>, actual: List<Map<String, Any?>>,
                            pk: List<String>, config: TestacoConfig): TableDiff? {
        val missing = mutableListOf<Map<String, Any?>>()
        val extra = mutableListOf<Map<String, Any?>>()
        val diffs = mutableListOf<RowDiff>()

        if (pk.isEmpty()) {
            for (i in expected.indices) {
                if (i >= actual.size) { missing.add(expected[i]); continue }
                compareRow(expected[i], actual[i], pk, config)?.let { diffs.add(it) }
            }
            if (actual.size > expected.size) extra.addAll(actual.drop(expected.size))
        } else {
            val actualByPk = actual.associateBy { row -> normalizePkKey(pk.map { row[it] }) }
            val expectedByPk = expected.associateBy { row -> normalizePkKey(pk.map { row[it] }) }

            for ((k, v) in expectedByPk) {
                val act = actualByPk[k]
                if (act == null) missing.add(v) else compareRow(v, act, pk, config)?.let { diffs.add(it) }
            }
            for ((k, v) in actualByPk) { if (k !in expectedByPk) extra.add(v) }
        }

        return if (missing.isEmpty() && extra.isEmpty() && diffs.isEmpty()) null
        else TableDiff(missing, extra, diffs)
    }

    private fun normalizePkKey(values: List<Any?>): List<Any?> {
        return values.map { v ->
            when (v) {
                is Number -> v.toLong()
                else -> v
            }
        }
    }

    private fun compareRow(expected: Map<String, Any?>, actual: Map<String, Any?>,
                           pk: List<String>, config: TestacoConfig): RowDiff? {
        val mismatches = mutableMapOf<String, ColumnMismatch>()
        for ((col, expVal) in expected) {
            val matcher = ColumnMatcher.forExpected(expVal, config.defaultTimestampTolerance)
            val actVal = actual[col]
            if (!matcher.matches(actVal))
                mismatches[col] = ColumnMismatch(expected = expVal, actual = actVal, message = matcher.describe(actVal))
        }
        return if (mismatches.isEmpty()) null
        else RowDiff(primaryKey = pk.associateWith { actual[it] }, columnMismatches = mismatches)
    }
}
