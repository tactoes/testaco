package org.testaco.dataset

import java.util.*

class DefaultTableMetaData : AbstractTableMetaData {

    override val tableName: String
    override val columns: List<Column>
    override val primaryKeys: List<Column>

    @JvmOverloads
    constructor(
        tableName: String,
        columns: List<Column>,
        primaryKeys: List<String> = listOf(),
    ) {
        this.tableName = tableName
        this.columns = columns
        this.primaryKeys = Columns.getColumns(primaryKeys, columns)
    }

    override fun toString(): String {
        return "tableName=" + tableName +
            ", columns=" + columns.map { c -> c.toString() }.joinToString(", ") +
            ", keys=" + primaryKeys.map { pk -> pk.toString() }.joinToString { ", " }
    }
}
