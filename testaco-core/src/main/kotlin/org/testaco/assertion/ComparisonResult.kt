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
 */package org.testaco.assertion

data class ComparisonResult(
    val matches: Boolean,
    val tableDiffs: Map<String, TableDiff> = emptyMap()
) {
    companion object {
        fun match() = ComparisonResult(matches = true)
    }
}

data class TableDiff(
    val missingRows: List<Map<String, Any?>> = emptyList(),
    val extraRows: List<Map<String, Any?>> = emptyList(),
    val columnDiffs: List<RowDiff> = emptyList()
)

data class RowDiff(
    val primaryKey: Map<String, Any?>,
    val columnMismatches: Map<String, ColumnMismatch>
)

data class ColumnMismatch(
    val expected: Any?,
    val actual: Any?,
    val message: String
)

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
