package org.testaco.dataset

import org.testaco.dataset.exceptions.DataSetException

interface ITable {
    val tableMetaData: ITableMetaData?
    val rowCount: Int

    /**
     * Returns this table value for the specified row and column.
     * @param row The row index, starting with 0
     * @param column The name of the column
     * @return The value
     *
     * @throws NoSuchColumnException if specified column name do not exist in
     * this table
     * @throws RowOutOfBoundsException if specified row is less than zero or
     * equals or greater than `getRowCount`
     */
    @Throws(DataSetException::class)
    fun getValue(row: Int, column: String?): Any?
}
