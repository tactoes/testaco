package org.testaco.dataset

interface IDataSet {
    val tableNames: Array<String>

    fun getTableMetaData(tableName: String): ITableMetaData

    fun getTable(tableName: String): ITableMetaData

    operator fun iterator(): ITableIterator

    fun reverseIterator(): ITableIterator

    val isCaseSensitiveTableNames: Boolean
}
