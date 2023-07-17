package org.testaco.database

import org.testaco.dataset.IDataSet
import org.testaco.dataset.ITable
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

interface IDatabaseConnection {
    @get:Throws(SQLException::class)
    val connection: Connection

    /**
     * Returns the database schema name.
     */
    val schema: String

    /**
     * Close this connection.
     */
    @Throws(SQLException::class)
    fun close()

    /**
     * Creates a dataset corresponding to the entire database.
     */
    @Throws(SQLException::class)
    fun createDataSet(): IDataSet?

    @Throws(SQLException::class)
    fun createDataSet(tableNames: Array<String>): IDataSet

    @Throws(SQLException::class)
    fun createQueryTable(tableName: String, sql: String): ITable

    @Throws(SQLException::class)
    fun createTable(tableName: String, preparedStatement: PreparedStatement): ITable

    @Throws(SQLException::class)
    fun createTable(tableName: String): ITable

    @Throws(SQLException::class)
    fun getRowCount(tableName: String): Int

    @Throws(SQLException::class)
    fun getRowCount(tableName: String, whereClause: String): Int

    val config: DatabaseConfig
}
