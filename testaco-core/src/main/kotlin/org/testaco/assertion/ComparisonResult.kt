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

package org.testaco.assertion

/**
 * Result of comparing database state against expected dataset.
 *
 * @param matches true if actual database exactly matches expected dataset
 * @param tableDiffs Map of table name to [TableDiff] (empty if matches is true)
 */
data class ComparisonResult(
    val matches: Boolean,
    val tableDiffs: Map<String, TableDiff> = emptyMap()
) {
    companion object {
        fun match() = ComparisonResult(matches = true)
    }
}

/**
 * Differences found in a single table.
 *
 * @param missingRows Rows in expected but not in actual database
 * @param extraRows Rows in actual database but not in expected
 * @param columnDiffs Rows that exist in both but have column value mismatches
 */
data class TableDiff(
    val missingRows: List<Map<String, Any?>> = emptyList(),
    val extraRows: List<Map<String, Any?>> = emptyList(),
    val columnDiffs: List<RowDiff> = emptyList()
)

/**
 * Column value mismatches within a single row.
 *
 * @param primaryKey The primary key of the row that has mismatches
 * @param columnMismatches Map of column name to [ColumnMismatch] details
 */
data class RowDiff(
    val primaryKey: Map<String, Any?>,
    val columnMismatches: Map<String, ColumnMismatch>
)

/**
 * Details of a single column mismatch.
 *
 * @param expected The expected value from the test dataset
 * @param actual The actual value from the database
 * @param message Human-readable description of the difference
 */
data class ColumnMismatch(
    val expected: Any?,
    val actual: Any?,
    val message: String
)

/**
 * Exception thrown when [Testaco.assertMatches] finds a dataset mismatch.
 *
 * The exception message includes detailed information about:
 * - Which tables don't match
 * - Which rows are missing or extra
 * - Which columns have mismatched values
 * - Path to the actual dump file (for easy diff)
 *
 * @param result Details of what didn't match
 * @param dumpPath Optional path to the dumped actual database state
 */
class DataSetMismatchException(
    val result: ComparisonResult,
    val dumpPath: String? = null
) : RuntimeException(formatMessage(result, dumpPath)) {
    companion object {
        private fun formatMessage(result: ComparisonResult, dumpPath: String?): String {
            val sb = StringBuilder("Dataset mismatch\n\n")
            for ((table, diff) in result.tableDiffs) {
                sb.appendLine("Table '$table':")
                for (rowDiff in diff.columnDiffs) {
                    sb.appendLine("  Row (PK: ${rowDiff.primaryKey}):")
                    for ((col, mismatch) in rowDiff.columnMismatches) {
                        sb.appendLine("    $col: ${mismatch.message}")
                    }
                }
                if (diff.missingRows.isNotEmpty()) sb.appendLine("  Missing rows: ${diff.missingRows.size}")
                if (diff.extraRows.isNotEmpty()) sb.appendLine("  Extra rows: ${diff.extraRows.size}")
            }
            if (dumpPath != null) {
                sb.appendLine("\nActual database state dumped to: $dumpPath")
            }
            return sb.toString()
        }
    }
}
