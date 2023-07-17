package org.testaco.dataset

interface ITable {
    val tableMetaData: ITableMetaData?
    val rowCount: Int
}
