package org.testaco.dataset

data class DataSet(
    val tables: Map<String, List<Map<String, Any?>>>
) {
    fun merge(other: DataSet): DataSet {
        val merged = tables.toMutableMap()
        for ((table, rows) in other.tables) {
            merged[table] = (merged[table] ?: emptyList()) + rows
        }
        return DataSet(merged)
    }

    companion object {
        fun empty() = DataSet(emptyMap())
    }
}
