package org.testaco.comparator

import org.testaco.*
import org.testaco.comparator.model.*

class DatasetComparator(val schema: TestacoSchema) {
  fun compare(database: DataSet?, reference: DataSet?): CDatasetResult {
    require(!(database == null || reference == null)) { "Datasets can not be null: $database, $reference" }

    return compareDataSet(database, reference)
  }

  private fun compareDataSet(database: DataSet, reference: DataSet): CDatasetResult = CDatasetResult(compareTables(
    database.tables, reference.tables))

  private fun compareTables(database: Set<TTable>, reference: Set<TTable>): Set<CTableResult> {
    val databaseTables = database.map { it.name }
    val referenceTables = reference.map { it.name }
    return databaseTables.minus(referenceTables).map { CTableMissingResult(it, "Table $it is missing in the reference data set") }
      .plus(referenceTables.minus(databaseTables).map { CTableMissingResult(it, "Table $it is missing in the database data set") })
      .plus(databaseTables.union(referenceTables).map { tableName -> compareTable(database.find { it.name == tableName }!!, reference.find { it.name == tableName }!!) }).toSet()
  }

  private fun compareTable(databaseTable: TTable, reference: TTable): CTableResult {
    val tableName = databaseTable.name
    val tabledef = schema.tables.find { it.tableName == tableName }!!
    val dbPks: Set<String> = tabledef.pks(databaseTable)
    val refPks: Set<String> = tabledef.pks(reference)
    val missingdb: List<CRowResult> = dbPks.minus(refPks).map { CRowMissingResult(tableName, it, "Row with primary key $it is missing in table ${databaseTable.name} in the reference data set") }
    val missingres: List<CRowResult> = refPks.minus(dbPks).map { CRowMissingResult(tableName, it, "Row with primary key $it is missing in table ${databaseTable.name} in the database data set")}
    check(!(databaseTable.rows.size == 0 && reference.rows.size > 0)) { "Reference data set contains data, but database table is empty" }
    val compareResult: List<CRowResult> = dbPks.union(refPks).flatMap { key ->
      databaseTable.rows.forEach { println("Row pk is ${it.pk()}") }
      val dbRow: Row = databaseTable.rows.find { it.pk().toString() == key } ?: throw IllegalStateException("Could not find db row for primary key $key, db rows are ${databaseTable.rows.map { it.pk().toString() }}")
      val refRow: Row = reference.rows.find { it.pk().toString() == key } ?: throw IllegalStateException("Could not find ref row for primary key $key, db rows are ${databaseTable.rows.map { it.pk().toString() }}")
      val results: List<CRowResult> = compareRows(tableName, dbRow, refRow)
      results
    }
    val results: List<CRowResult> = missingdb
      .plus(missingres)
      .plus(compareResult)

    if (results.isEmpty()) {
      return CTableOkResult(databaseTable.name)
    }
    return CTableRowMismatchResult(databaseTable.name, results.toSet())
  }

  private fun compareRows(tableName: String, dbRow: Row, refRow: Row): List<CRowResult> {
    val dbColNames = dbRow.columns.map { it.name }
    val refColNames = refRow.columns.map { it.name }
    return dbColNames.minus(refColNames).map { columnName: String ->
      CColumnMissingResult(tableName, columnName, "Column with name ${columnName} is missing from reference data set row with id ${dbRow.pk()}")
    }.plus(refColNames.minus(dbColNames).map { columnName: String ->
      CColumnMissingResult(tableName, columnName, "Column with name ${columnName} is missing from database data set row with id ${dbRow.pk()}")
    }).plus(refColNames.union(dbColNames).flatMap { columnName: String ->
        val dbCol = dbRow.columns.find { it.name == columnName } ?: throw IllegalStateException("Could not find column with name $columnName in db row $dbRow")
        val refCol = refRow.columns.find { it.name == columnName } ?: throw IllegalStateException("Could not find column with name $columnName in ref row $refRow")
        val dbPk = dbRow.pk().toString()
        //TODO: Proper, typed comparisons
        listOf(
          when {
            dbCol.value == null && refCol.value == null -> {
              null //Null value in both db and reference, all good
            }
            dbCol.value == null && refCol.value != null -> {
              CColumnResult(tableName, columnName, "Database contains null value, but reference data contains ${refCol.value} for primary key $dbPk and column $columnName in table $tableName")
            }
            refCol.value == null -> {
              CColumnResult(tableName, columnName, "Reference data set contains null value, but database contains ${dbCol.value} for primary key $dbPk and column $columnName in table $tableName")
            }
            !refCol.value.toString().equals(dbCol.value.toString()) -> {
              println("Value check: ${refCol.value} and ${dbCol.value}, ${refCol.value.javaClass}, ${dbCol.value?.javaClass}")
              CColumnResult(tableName, columnName, "Database value ${dbCol.value} does not match reference value ${refCol.value} for primary key $dbPk and column $columnName in table $tableName")
            }
            else -> {
              null // Matching values, all good
            }
          }
        ).filterNotNull()
      })
  }
}