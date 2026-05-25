package org.testaco.schema

import org.testaco.config.PgFixturesConfig
import org.testaco.dataset.DataSet

class SchemaValidationException(message: String) : RuntimeException(message)
class DataSetValidationException(message: String) : RuntimeException(message)

object SchemaValidator {

    fun validateSchemaMatch(expected: Schema, live: Schema, config: PgFixturesConfig) {
        val errors = mutableListOf<String>()
        val expectedTables = expected.tables.keys
        val liveTables = live.tables.keys

        val missing = expectedTables - liveTables
        val extra = liveTables - expectedTables
        if (missing.isNotEmpty()) errors.add("Missing tables: $missing")
        if (extra.isNotEmpty()) errors.add("Extra tables: $extra")

        for (tableName in expectedTables.intersect(liveTables)) {
            val et = expected.tables[tableName]!!
            val lt = live.tables[tableName]!!
            val eCols = et.columns.filterKeys { !config.isColumnIgnored(tableName, it) }
            val lCols = lt.columns.filterKeys { !config.isColumnIgnored(tableName, it) }

            val missingCols = eCols.keys - lCols.keys
            val extraCols = lCols.keys - eCols.keys
            if (missingCols.isNotEmpty()) errors.add("$tableName: missing columns: $missingCols")
            if (extraCols.isNotEmpty()) errors.add("$tableName: extra columns: $extraCols")

            for (col in eCols.keys.intersect(lCols.keys)) {
                val ec = eCols[col]!!; val lc = lCols[col]!!
                if (ec.type != lc.type)
                    errors.add("$tableName.$col: expected ${ec.type}, found ${lc.type}")
                if (ec.nullable != lc.nullable)
                    errors.add("$tableName.$col: expected nullable=${ec.nullable}, found nullable=${lc.nullable}")
            }
        }

        if (errors.isNotEmpty())
            throw SchemaValidationException("Database schema does not match testaco-schema.json\n\n  ${errors.joinToString("\n  ")}")
    }

    fun validateDataSetAgainstSchema(dataSet: DataSet, schema: Schema, config: PgFixturesConfig) {
        val errors = mutableListOf<String>()
        for ((tableName, rows) in dataSet.tables) {
            val tableDef = schema.tables[tableName]
            if (tableDef == null) { errors.add("Dataset references unknown table '$tableName'"); continue }
            val validCols = tableDef.columns.keys.filterNot { config.isColumnIgnored(tableName, it) }.toSet()
            for (row in rows) {
                for (col in row.keys) {
                    if (config.isColumnIgnored(tableName, col)) continue
                    if (col !in validCols) {
                        val suggestion = validCols.minByOrNull { levenshtein(it, col) }
                        val hint = if (suggestion != null) " Did you mean '$tableName.$suggestion'?" else ""
                        errors.add("Dataset references unknown column '$tableName.$col'.$hint")
                    }
                }
            }
        }
        if (errors.isNotEmpty()) throw DataSetValidationException(errors.joinToString("\n"))
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length)
            dp[i][j] = minOf(dp[i-1][j]+1, dp[i][j-1]+1, dp[i-1][j-1] + if (a[i-1]==b[j-1]) 0 else 1)
        return dp[a.length][b.length]
    }
}
