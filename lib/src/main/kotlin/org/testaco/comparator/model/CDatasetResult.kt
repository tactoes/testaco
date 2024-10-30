package org.testaco.comparator.model

/**
 * CResults imply there is a false result somewhere.
 */
fun interface CResult {
  fun equals(): Boolean
}

data class CDatasetResult(val tableResults: Set<CTableResult>): CResult {
  override fun equals(): Boolean {
    return tableResults.all { table: CTableResult -> table.equals()}
  }
  fun errorMessage(): String {
    return tableResults.map { it.toString() }.joinToString("\n")
  }
}

interface CTableResult: CResult

data class CTableMissingResult(val tableName: String, val message: String): CTableResult {
  override fun equals(): Boolean = false
}
data class CTableRowMismatchResult(val table: String, val rowResults: Set<CRowResult>): CTableResult {
  override fun equals(): Boolean = rowResults.isEmpty()
}
data class CTableOkResult(val tableName: String): CTableResult {
  override fun equals(): Boolean = true
}

interface CRowResult: CResult

data class CRowMissingResult(val table: String, val rowId: String, val message: String): CRowResult {
  override fun equals(): Boolean = false
}

data class CColumnResult(val table: String, val name: String, val message: String): CRowResult {
  override fun equals(): Boolean = true
}
data class CColumnMissingResult(val table: String, val name: String, val message: String): CRowResult {
  override fun equals(): Boolean = true
}