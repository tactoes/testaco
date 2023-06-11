package org.testaco.dataset

import org.slf4j.LoggerFactory
import org.testaco.dataset.exceptions.RowOutOfBoundsException

abstract class AbstractTable : ITable {
    protected fun assertValidRowIndex(row: Int) {
        if (logger.isDebugEnabled) {
            logger.debug(
                "assertValidRowIndex(row={}) - start", Integer
                    .toString(row)
            )
        }
        assertValidRowIndex(row, rowCount)
    }

    protected fun assertValidRowIndex(row: Int, rowCount: Int) {
        if (logger.isDebugEnabled) {
            logger.debug(
                "assertValidRowIndex(row={}, rowCount={}) - start",
                Integer.toString(row), Integer.toString(rowCount)
            )
        }
        if (row < 0) {
            throw RowOutOfBoundsException("$row < 0")
        }
        if (row >= rowCount) {
            throw RowOutOfBoundsException("$row >= $rowCount")
        }
    }

    protected fun assertValidColumn(columnName: String?) {
        logger.debug("assertValidColumn(columnName={}) - start", columnName)
        val metaData = tableMetaData!!
        // Try to find the column in the metadata - if it cannot be found an
        // exception is thrown
        Columns.getColumnValidated(
            columnName, metaData.columns, metaData
                .tableName
        )
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
