package org.testaco.database

import org.testaco.dataset.ITableMetaData
import java.sql.PreparedStatement
import java.sql.SQLException

interface IResultSetTableFactory {
    @Throws(SQLException::class)
    fun createTable(
        tableName: String, selectStatement: String,
        connection: IDatabaseConnection
    ): IResultSetTable

    @Throws(SQLException::class)
    fun createTable(
        metaData: ITableMetaData,
        connection: IDatabaseConnection
    ): IResultSetTable

    @Throws(SQLException::class)
    fun createTable(
        tableName: String, preparedStatement: PreparedStatement,
        connection: IDatabaseConnection
    ): IResultSetTable
}
