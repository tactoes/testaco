package org.testaco.dataset

open class CachedTable : DefaultTable {
    constructor(table: ITable) : super(table.tableMetaData) {
        addTableRows(table)
    }

    protected constructor(metaData: ITableMetaData?) : super(metaData)
}
