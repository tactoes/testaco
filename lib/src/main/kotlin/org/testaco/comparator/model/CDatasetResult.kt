package org.testaco.comparator.model

/**
 * CResults imply there is a false result somewhere.
 */
interface CResult {
  fun equals(): Boolean
}

data class CDatasetResult(val tableResults: Set<CTableResult>): CResult {
  override fun equals(): Boolean = tableResults.isNotEmpty()
}

interface CTableResult: CResult

data class CTableMissingResult(val tableName: String, val message: String): CTableResult {
  override fun equals(): Boolean = false
}
data class CTableRowMismatchResult(val table: String, val rowResults: Set<CRowResult>): CTableResult {
  override fun equals(): Boolean = rowResults.isNotEmpty()
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