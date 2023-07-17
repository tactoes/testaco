package org.testaco.dataset

import org.slf4j.LoggerFactory

open class DefaultTable : AbstractTable {

    override val tableMetaData: ITableMetaData?
    private val _rowList: MutableList<Column>?

    constructor(tableName: String) {
        tableMetaData = DefaultTableMetaData(tableName, listOf())
        _rowList = ArrayList()
    }

    constructor(tableName: String, columns: List<Column>) {
        tableMetaData = DefaultTableMetaData(tableName, columns)
        _rowList = ArrayList()
    }

    constructor(metaData: ITableMetaData?) {
        tableMetaData = metaData
        _rowList = ArrayList()
    }

    fun addRow(values: Array<Column>) {
        _rowList!!.addAll(values)
    }

    fun addTableRows(table: ITable) {
        val columns = tableMetaData!!.columns
        if (columns.size <= 0) {
            logger.warn("The table '$table' does not have any columns. Cannot add table rows. This should never happen...")
            return
        }
        columns.mapIndexed { index, column: Column -> _rowList!!.add(column) }
    }

    override val rowCount: Int
        get() = _rowList!!.size

    override fun toString(): String {
        val sb = StringBuffer()
        sb.append(javaClass.name).append("[")
        sb.append("_metaData=").append(if (tableMetaData == null) "null" else tableMetaData.toString())
        sb.append(", _rowList.size()=").append(if (_rowList == null) "null" else "" + _rowList.size)
        sb.append("]")
        return sb.toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultTable::class.java)
    }
}
