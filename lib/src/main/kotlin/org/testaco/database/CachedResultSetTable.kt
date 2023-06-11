package org.testaco.database

import org.testaco.dataset.CachedTable
import org.testaco.dataset.ITableMetaData

class CachedResultSetTable(table: IResultSetTable) : CachedTable(table.tableMetaData), IResultSetTable {
    init {
        try {
            addTableRows(table)
        } finally {
            table.close()
        }
    }

    override fun close() {
    }
}
