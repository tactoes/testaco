package org.testaco.dataset

import org.testaco.dataset.exceptions.DataSetException

/**
 * Represents a collection of tables.
 *
 * @author Manuel Laflamme
 * @version $Revision$
 * @since Feb 17, 2002
 */
interface IDataSet {
    @get:Throws(DataSetException::class)
    val tableNames: Array<String?>?

    /**
     * Returns the specified table metadata.
     *
     * @throws AmbiguousTableNameException if dataset contains multiple tables
     * having the specified name. Use [.iterator] to access
     * to all tables.
     * @throws NoSuchTableException if dataset do not contains the specified
     * table
     */
    @Throws(DataSetException::class)
    fun getTableMetaData(tableName: String?): ITableMetaData?

    /**
     * Returns the specified table.
     *
     * @throws AmbiguousTableNameException if dataset contains multiple tables
     * having the specified name. Use [.iterator] to access
     * to all tables.
     * @throws NoSuchTableException if dataset do not contains the specified
     * table
     */
    @Throws(DataSetException::class)
    fun getTable(tableName: String?): ITable?

    @get:Throws(DataSetException::class)
    @get:Deprecated("Use {@link #iterator} or {@link #reverseIterator} instead.")
    val tables: Array<Any?>?

    /**
     * Returns an iterator over the tables in this dataset in proper sequence.
     */
    @Throws(DataSetException::class)
    operator fun iterator(): ITableIterator?

    /**
     * Returns an iterator over the tables in this dataset in reverse sequence.
     */
    @Throws(DataSetException::class)
    fun reverseIterator(): ITableIterator?

    /**
     * Whether or not this dataset handles table names in a case sensitive way or not.
     * @return `true` if the case sensitivity of table names is used in this dataset.
     * @since 2.4.2
     */
    val isCaseSensitiveTableNames: Boolean
}
