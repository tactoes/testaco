package org.testaco.database

import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.SQLException

interface IMetadataHandler {
    @Throws(SQLException::class)
    fun getColumns(databaseMetaData: DatabaseMetaData, schemaName: String, tableName: String): ResultSet

    /**
     * Checks if the given `resultSet` matches the given schema and table name.
     * The comparison is **case sensitive**.
     */
    @Throws(SQLException::class)
    fun matches(
        resultSet: ResultSet, catalog: String, schema: String,
        table: String, column: String, caseSensitive: Boolean
    ): Boolean

    /**
     * Returns the schema name to which the table of the current result set index belongs.
     * @param resultSet The result set pointing to a valid record in the database that was returned
     * by [DatabaseMetaData.getTables].
     */
    @Throws(SQLException::class)
    fun getSchema(resultSet: ResultSet): String

    @Throws(SQLException::class)
    fun tableExists(databaseMetaData: DatabaseMetaData, schemaName: String, tableName: String): Boolean

    /**
     * Returns the tables in the given schema that matches one of the given tableTypes.
     */
    @Throws(SQLException::class)
    fun getTables(databaseMetaData: DatabaseMetaData, schemaName: String, tableTypes: Array<String?>): ResultSet?

    @Throws(SQLException::class)
    fun getPrimaryKeys(databaseMetaData: DatabaseMetaData, schemaName: String, tableName: String): ResultSet?
}
