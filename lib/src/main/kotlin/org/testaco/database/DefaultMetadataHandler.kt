package org.testaco.database

import org.slf4j.LoggerFactory
import org.testaco.util.SQLHelper
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.SQLException

class DefaultMetadataHandler : IMetadataHandler {
    @Throws(SQLException::class)
    override fun getColumns(
        databaseMetaData: DatabaseMetaData,
        schemaName: String,
        tableName: String,
    ): ResultSet {
        if (logger.isTraceEnabled) {
            logger.trace(
                "getColumns(databaseMetaData={}, schemaName={}, tableName={}) - start",
                *arrayOf<Any>(databaseMetaData, schemaName, tableName),
            )
        }
        return databaseMetaData.getColumns(
            null,
            schemaName,
            tableName,
            "%",
        )
    }

    @Throws(SQLException::class)
    override fun matches(
        resultSet: ResultSet,
        catalog: String,
        schema: String,
        table: String,
        column: String,
        caseSensitive: Boolean,
    ): Boolean {
        if (logger.isTraceEnabled) {
            logger.trace(
                "matches(columnsResultSet={}, catalog={}, schema={}," +
                    " table={}, column={}, caseSensitive={}) - start",
                *arrayOf<Any?>(
                    resultSet,
                    catalog,
                    schema,
                    table,
                    column,
                    java.lang.Boolean.valueOf(caseSensitive),
                ),
            )
        }
        val catalogName: String = resultSet.getString(1)
        val schemaName: String = resultSet.getString(2)
        val tableName: String = resultSet.getString(3)
        val columnName: String = resultSet.getString(4)
        if (logger.isDebugEnabled) {
            logger.debug(
                "Comparing the following values using caseSensitive={} (searched<=>actual): " +
                    "catalog: {}<=>{} schema: {}<=>{} table: {}<=>{} column: {}<=>{}",
                *arrayOf<Any?>(
                    java.lang.Boolean.valueOf(caseSensitive),
                    catalog, catalogName,
                    schema, schemaName,
                    table, tableName,
                    column, columnName,
                ),
            )
        }
        return areEqualIgnoreNull(catalog, catalogName, caseSensitive) &&
            areEqualIgnoreNull(schema, schemaName, caseSensitive) &&
            areEqualIgnoreNull(table, tableName, caseSensitive) &&
            areEqualIgnoreNull(column, columnName, caseSensitive)
    }

    private fun areEqualIgnoreNull(
        value1: String?,
        value2: String,
        caseSensitive: Boolean,
    ): Boolean {
        return SQLHelper.areEqualIgnoreNull(value1, value2, caseSensitive)
    }

    @Throws(SQLException::class)
    override fun getSchema(resultSet: ResultSet): String {
        if (logger.isTraceEnabled) {
            logger.trace(
                "getColumns(resultSet={}) - start",
                resultSet,
            )
        }
        return resultSet.getString(2)
    }

    @Throws(SQLException::class)
    override fun tableExists(databaseMetaData: DatabaseMetaData, schemaName: String, tableName: String): Boolean {
        if (logger.isTraceEnabled) {
            logger.trace(
                "tableExists(metaData={}, schemaName={}, tableName={}) - start",
                *arrayOf<Any>(databaseMetaData, schemaName, tableName),
            )
        }
        val tableRs: ResultSet = databaseMetaData.getTables(null, schemaName, tableName, null)
        return try {
            tableRs.next()
        } finally {
            SQLHelper.close(tableRs)
        }
    }

    @Throws(SQLException::class)
    override fun getTables(databaseMetaData: DatabaseMetaData, schemaName: String, tableTypes: Array<String?>): ResultSet {
        if (logger.isTraceEnabled) {
            logger.trace(
                "getTables(metaData={}, schemaName={}, tableType={}) - start",
                *arrayOf<Any>(databaseMetaData, schemaName, tableTypes),
            )
        }
        return databaseMetaData.getTables(null, schemaName, "%", tableTypes)
    }

    @Throws(SQLException::class)
    override fun getPrimaryKeys(databaseMetaData: DatabaseMetaData, schemaName: String, tableName: String): ResultSet {
        if (logger.isTraceEnabled) {
            logger.trace(
                "getPrimaryKeys(metaData={}, schemaName={}, tableName={}) - start",
                databaseMetaData,
                schemaName,
                tableName,
            )
        }
        return databaseMetaData.getPrimaryKeys(
            null,
            schemaName,
            tableName,
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultMetadataHandler::class.java)
    }
}
