package org.testaco.dataset

import org.slf4j.LoggerFactory
import org.testaco.dataset.DefaultTable
import org.testaco.dataset.exceptions.RowOutOfBoundsException

open class DefaultTable : AbstractTable {

    override val tableMetaData: ITableMetaData?
    private val _rowList: MutableList<Column>?

    constructor(tableName: String) {
        tableMetaData = DefaultTableMetaData(tableName, arrayOf())
        _rowList = ArrayList()
    }

    constructor(tableName: String, columns: Array<Column>) {
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
        try {
            val columns = tableMetaData!!.columns
            if (columns.size <= 0) {
                logger.warn("The table '$table' does not have any columns. Cannot add table rows. This should never happen...")
                return
            }
            var i = 0
            while (true) { //FIXME: Come on, terminating on an exception?!
                val rowValues = arrayOf<Column>()
                columns.forEachIndexed { index, column -> rowValues[index] = table.getValue(i, column.columnName) }
                _rowList!!.add(rowValues)
                i++
            }
        } catch (e: RowOutOfBoundsException) {
            // end of table
            // ignore error.
        }
    }

    fun setValue(row: Int, column: String, value: Any): Any {
        assertValidRowIndex(row)
        val rowValues = _rowList!![row] as Array<Any>
        val columnIndex: Int = getColumnIndex(column)
        val oldValue = rowValues[columnIndex]
        rowValues[columnIndex] = value
        return oldValue
    }

    override val rowCount: Int
        get() = _rowList!!.size

    override fun getValue(row: Int, column: String?): Any {
        if (logger.isDebugEnabled) logger.debug("getValue(row={}, column={}) - start", Integer.toString(row), column)
        assertValidRowIndex(row)
        val rowValues = _rowList!![row] as Array<Any>
        return rowValues[getColumnIndex(column)]
    }

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
