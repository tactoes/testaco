package org.testaco.dataset

abstract class AbstractDataSet(override val isCaseSensitiveTableNames: Boolean = false) : IDataSet {

    protected var _orderedTableNameMap: OrderedTableNameMap = run {
        val _orderedTableNameMap = OrderedTableNameMap(isCaseSensitiveTableNames)
        val iterator: ITableIterator = createIterator(false)
        while (iterator.next()) {
            val table: ITable = iterator.table
            _orderedTableNameMap.add(table.tableMetaData!!.tableName, table.tableMetaData!!)
        }
        _orderedTableNameMap
    }

    protected abstract fun createIterator(reversed: Boolean): ITableIterator

    override val tableNames: Array<String>
        get() {
            return _orderedTableNameMap.tableNames
        }

    override fun getTableMetaData(tableName: String): ITableMetaData {
        return getTable(tableName)
    }

    override fun getTable(tableName: String): ITableMetaData {
        return _orderedTableNameMap.get(tableName)
    }

    override fun iterator(): ITableIterator {
        return createIterator(false)
    }

    override fun reverseIterator(): ITableIterator {
        return createIterator(true)
    }

    override fun toString(): String {
        val sb = StringBuffer()
        sb.append("AbstractDataSet[")
        sb.append("_orderedTableNameMap=").append(_orderedTableNameMap)
        sb.append("]")
        return sb.toString()
    }
}
