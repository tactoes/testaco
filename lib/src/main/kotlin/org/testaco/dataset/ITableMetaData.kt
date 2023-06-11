package org.testaco.dataset

interface ITableMetaData {
    val tableName: String
    val columns: List<Column>
    val primaryKeys: List<Column>
    fun getColumnIndex(columnName: String): Int
}
