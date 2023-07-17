package org.testaco.dataset

import org.slf4j.LoggerFactory
import org.testaco.dataset.exceptions.NoSuchColumnException
import org.testaco.dataset.exceptions.RowOutOfBoundsException

abstract class AbstractTable : ITable {
    protected fun assertValidRowIndex(row: Int) {
        if (logger.isDebugEnabled) {
            logger.debug(
                "assertValidRowIndex(row={}) - start",
                row.toString(),
            )
        }
        assertValidRowIndex(row, rowCount)
    }

    protected fun assertValidRowIndex(row: Int, rowCount: Int) {
        if (logger.isDebugEnabled) {
            logger.debug(
                "assertValidRowIndex(row={}, rowCount={}) - start",
                row.toString(),
                rowCount.toString(),
            )
        }
        if (row < 0) {
            throw RowOutOfBoundsException("$row < 0")
        }
        if (row >= rowCount) {
            throw RowOutOfBoundsException("$row >= $rowCount")
        }
    }

    protected fun assertValidColumn(columnName: String) {
        if (Columns.getColumn(columnName, tableMetaData!!.columns) == null) {
            throw NoSuchColumnException(tableMetaData!!.tableName, columnName)
        }
    }

    protected fun getColumnIndex(columnName: String?): Int {
        logger.debug("getColumnIndex(columnName={}) - start", columnName)
        val metaData = tableMetaData!!
        return metaData.getColumnIndex(columnName!!)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractTable::class.java)
    }
}
