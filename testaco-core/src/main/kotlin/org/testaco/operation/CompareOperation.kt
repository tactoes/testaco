package org.testaco.operation

import org.testaco.assertion.*
import org.testaco.config.TestacoConfig
import org.testaco.dataset.DataSet
import org.testaco.schema.Schema
import java.sql.Connection

object CompareOperation {

    fun execute(connection: Connection, expected: DataSet, schema: Schema, config: TestacoConfig): ComparisonResult {
        val tableDiffs = mutableMapOf<String, TableDiff>()
        var allMatch = true

        for ((tableName, expectedRows) in expected.tables) {
            val tableDef = schema.tables[tableName] ?: continue
            val pk = tableDef.primaryKey
            val (s, t) = tableName.split(".", limit = 2)

            val orderBy = if (pk.isNotEmpty()) pk.joinToString(", ") { "\"$it\"" } else "1"

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

            val filtered = expectedRows.map { it.filterKeys { k -> !config.isColumnIgnored(tableName, k) } }
            val diff = compareRows(filtered, actualRows, pk, config)
            if (diff != null) { tableDiffs[tableName] = diff; allMatch = false }
        }

        return if (allMatch) ComparisonResult.match() else ComparisonResult(false, tableDiffs)
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
