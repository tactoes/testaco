package org.testaco.dataset

import java.util.*

class DefaultTableMetaData : AbstractTableMetaData {

    override val tableName: String
    override val columns: Array<Column>
    override val primaryKeys: Array<Column>

    @JvmOverloads
    constructor(
        tableName: String, columns: Array<Column>,
        primaryKeys: Array<String?>? = arrayOfNulls(0)
    ) //throws DataSetException
    {
        this.tableName = tableName
        this.columns = columns
        this.primaryKeys = Columns.getColumns(primaryKeys, columns)
    }

    constructor(
        tableName: String, columns: Array<Column>,
        primaryKeys: Array<Column>
    ) //throws DataSetException
    {
        this.tableName = tableName
        this.columns = columns
        this.primaryKeys = primaryKeys
    }

    override fun toString(): String {
        return "tableName=" + tableName +
                ", columns=" + Arrays.asList(*columns) +
                ", keys=" + Arrays.asList(*primaryKeys) + ""
    }
}
