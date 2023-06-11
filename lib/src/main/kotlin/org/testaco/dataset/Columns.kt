package org.testaco.dataset

import org.slf4j.LoggerFactory
import org.testaco.dataset.filter.IColumnFilter
import java.util.*

object Columns {
    private val logger = LoggerFactory.getLogger(Columns::class.java)
    private val COLUMN_COMPARATOR = ColumnComparator()
    private val EMPTY_COLUMNS = listOf<Column>()

    fun getColumns(columnNames: List<String>, columns: List<Column>, tableName: String): List<Column> {
        if (columnNames.size == 0) {
            return EMPTY_COLUMNS
        }
        return columnNames.map { getColumn(it, columns) }.filterNotNull()
    }

    fun findColumnsByName(
        columnNames: List<String>,
        tableMetaData: ITableMetaData
    ): List<Column> {
        return columnNames.map { name: String -> tableMetaData.columns[tableMetaData.getColumnIndex(name)] }
    }

    fun findColumnsByName(
        columns: List<Column>,
        tableMetaData: ITableMetaData
    ): List<Column> {
        return findColumnsByName(columns.map { it.columnName }, tableMetaData)
    }

    fun getColumn(columnName: String, columns: List<Column>): Column? {
        return columns.find { column: Column -> columnName.equals(column.columnName, true) }
    }

    fun getColumns(
        tableName: String, columns: Array<Column>,
        columnFilter: IColumnFilter
    ): Collection<Column> {
        return columns.filter { column: Column -> columnFilter.accept(tableName, column) }
    }

    fun getSortedColumns(metaData: ITableMetaData): List<Column?> {
        return metaData.columns.sortedWith(COLUMN_COMPARATOR)
    }

    fun getColumnNames(columns: List<Column>): List<String?> {
        return columns.map { it.columnName }
    }

    fun getColumnNamesAsString(columns: List<Column>): String {
        return getColumnNames(columns).toString()
    }

    fun mergeColumnsByName(referenceColumns: List<Column>, columnsToMerge: List<Column>): List<Column> {
        val referenceNames = referenceColumns.map { it.columnName }
        return referenceColumns.plus(columnsToMerge.filterNot { column: Column -> referenceNames.contains(column.columnName)  })
    }

    fun getColumnDiff(
        expectedMetaData: ITableMetaData,
        actualMetaData: ITableMetaData
    ): ColumnDiff {
        return ColumnDiff(expectedMetaData, actualMetaData)
    }

    private class ColumnComparator : Comparator<Column> {
        override fun compare(column1: Column, column2: Column): Int {
            return column1.columnName.compareTo(column2.columnName, ignoreCase = true)
        }
   }

    data class ColumnDiff(
        val expectedMetaData: ITableMetaData,
        val actualMetaData: ITableMetaData
    ) {
        fun expected(): List<Column> = expectedMetaData.columns.minus(actualMetaData.columns)
        fun actual(): List<Column> = actualMetaData.columns.minus(expectedMetaData.columns)
        fun hasDifference(): Boolean = expectedMetaData.columns.equals(actualMetaData.columns)

        fun expectedAsString(): String = getColumnNamesAsString(expectedMetaData.columns)
        fun actualAsString(): String = getColumnNamesAsString(actualMetaData.columns)

        fun message(): String {
            return if (!hasDifference()) {
                "no difference found"
            } else {
                val message: String
                message = if (expectedMetaData.columns.size != actualMetaData.columns.size) {
                    "column count (table=" + expectedMetaData.tableName + ", " +
                            "expectedColCount=" + expectedMetaData.columns.size + ", actualColCount=" + actualMetaData.columns.size + ")"
                } else {
                    "column mismatch (table=${expectedMetaData.tableName})"
                }
                message
            }
        }

        override fun toString(): String =
            """[expected=${expectedMetaData.columns.toString()}, actual=${actualMetaData.columns.toString()}]"""

    }
}
