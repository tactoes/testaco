package org.testaco.dataset

interface ITableIterator {
    operator fun next(): Boolean
    val tableMetaData: ITableMetaData
    val table: ITable
}
