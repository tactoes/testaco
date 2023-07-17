package org.testaco.dataset

import org.testaco.database.exceptions.AmbiguousTableNameException
import org.testaco.dataset.exceptions.NoSuchTableException

class OrderedTableNameMap(val caseSensitiveTableNames: Boolean = false) {
    // TODO: Rewrite this to something more elegant.

    private val _tableMap = HashMap<String, ITableMetaData>()

    /**
     * Chronologically ordered list of table names - keeps the order
     * in which the table names have been added as well as the case in
     * which the table has been added
     */
    private val _tableNames = ArrayList<String>()
    private var _lastTableNameOverride: String? = null

    operator fun get(tableName: String): ITableMetaData {
        val correctedCaseTableName = getTableName(tableName)
        return _tableMap[correctedCaseTableName] ?: throw NoSuchTableException(tableName)
    }

    val tableNames: Array<String>
        /**
         * Provides the ordered table names having the same order in which the table
         * names have been added via [.add].
         * @return The list of table names ordered in the sequence as
         * they have been added to this map
         */
        get() = _tableNames.toTypedArray()

    fun containsTable(tableName: String): Boolean {
        val correctedCaseTableName = getTableName(tableName)
        return _tableMap.containsKey(correctedCaseTableName)
    }

    fun isLastTable(tableName: String): Boolean {
        return if (_tableNames.size == 0) {
            false
        } else {
            val lastTable = lastTableName
            lastTable?.let(::getTableName) == getTableName(tableName)
        }
    }

    val lastTableName: String?
        get() {
            if (_lastTableNameOverride != null) {
                return _lastTableNameOverride
            }
            return if (_tableNames.size > 0) {
                _tableNames[_tableNames.size - 1]
            } else {
                null
            }
        }

    fun setLastTable(tableName: String) {
        if (!containsTable(tableName)) {
            throw NoSuchTableException(tableName)
        }
        _lastTableNameOverride = tableName
    }

    fun add(tableName: String, table: ITableMetaData) {
        val tableNameCorrectedCase = getTableName(tableName)

        if (containsTable(tableNameCorrectedCase)) {
            throw AmbiguousTableNameException(tableNameCorrectedCase)
        } else {
            _tableMap[tableNameCorrectedCase] = table
            _tableNames.add(tableName)

            // Reset the override of the lastTableName
            _lastTableNameOverride = null
        }
    }

    // TODO No ordering going on here. Returned in whatever order. Refactor!
    fun orderedValues(): Collection<ITableMetaData> {
        return _tableMap.values
    }

    fun update(tableName: String, table: ITableMetaData) {
        require(containsTable(getTableName(tableName))) { "The table name '$tableName' does not exist in the map" }
        _tableMap[getTableName(tableName)] = table
    }

    fun getTableName(tableName: String): String {
        return if (!caseSensitiveTableNames) {
            tableName.uppercase()
        } else {
            tableName
        }
    }

    override fun toString(): String {
        val sb = StringBuffer()
        sb.append(javaClass.name).append("[")
        sb.append("_tableNames=").append(_tableNames)
        sb.append(", _tableMap=").append(_tableMap)
        sb.append(", _caseSensitiveTableNames=").append(caseSensitiveTableNames)
        sb.append("]")
        return sb.toString()
    }
}
