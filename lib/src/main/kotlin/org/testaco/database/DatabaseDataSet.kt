package org.testaco.database

import org.slf4j.LoggerFactory
import org.testaco.DatabaseUnitRuntimeException
import org.testaco.dataset.*
import org.testaco.dataset.exceptions.DataSetException
import org.testaco.dataset.filter.ITableFilterSimple
import org.testaco.util.QualifiedTableName
import org.testaco.util.SQLHelper
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.SQLException

class DatabaseDataSet @JvmOverloads constructor(
    val connection: IDatabaseConnection,
    val caseSensitiveTableNames: Boolean,
    val tableFilter: ITableFilterSimple? = null
) : AbstractDataSet(caseSensitiveTableNames) {
    /*    private var _tableMap: OrderedTableNameMap? = null
    private val _schemaSet = SchemaSet(caseSensitiveTableNames)

    internal constructor(connection: IDatabaseConnection) : this(
        connection,
        connection.config.caseSensitiveTableNames
    )

    private fun initialize(schema: String) {
        var schema = schema
        logger.debug("initialize() - start")
        val config = connection.config
        val qualifiedTableNamesActive = config.qualifiedTableNames
        if (schema == null || !qualifiedTableNamesActive) {
            schema = connection.schema
        }
        if (_tableMap != null && _schemaSet.contains(schema)) {
            return
        }
        try {
            logger.debug("Initializing the data set from the database...")
            val jdbcConnection = connection.connection
            val databaseMetaData: DatabaseMetaData = jdbcConnection.metaData
            val metadataHandler = config.metaDataHandler
            val resultSet: ResultSet? = metadataHandler.getTables(databaseMetaData, schema, arrayOf("table"))
            if (logger.isDebugEnabled) {
                logger.debug(SQLHelper.getDatabaseInfo(jdbcConnection.metaData))
                logger.debug("metadata resultset={}", resultSet)
            }
            try {
                _schemaSet.add(schema)
                if (resultSet != null) {
                    while (resultSet.next()) {
                        val schemaName = metadataHandler.getSchema(resultSet)
                        var tableName: String = resultSet.getString(3)
                        if (tableFilter != null && !tableFilter.accept(tableName)) {
                            logger.debug("Skipping table '{}'", tableName)
                            continue
                        }
                        if (schema == null && !_schemaSet.contains(schemaName)) {
                            _schemaSet.add(schemaName)
                        }

                        // Put the table into the table map
                        _tableMap?.add(tableName, null)
                    }
                }
            } finally {
                resultSet?.close()
            }
        } catch (e: SQLException) {
            throw DataSetException("Caught sql exception: ${e.toString()}", e)
        }
    }

    private val defaultSchema: String
        private get() = connection.schema

    override protected fun createIterator(reversed: Boolean): ITableIterator {
        var names = tableNames
        if (reversed) {
            names = DataSetUtils.reverseStringArray(names)
        }
        return DatabaseTableIterator(names, this)
    }

    val tableNames: Array<String>
        ////////////////////////////////////////////////////////////////////////////
        get() {
            initialize(null)
            return _tableMap.tableNames
        }

    @Throws(DataSetException::class)
    fun getTableMetaData(tableName: String?): ITableMetaData {
        logger.debug("getTableMetaData(tableName={}) - start", tableName)
        val qualifiedTableName = QualifiedTableName(tableName, defaultSchema)
        initialize(qualifiedTableName.getSchema())

        // Verify if table exist in the database
        if (!_tableMap.containsTable(tableName)) {
            logger.error(
                "Table '{}' not found in tableMap={}", tableName,
                _tableMap
            )
            throw NoSuchTableException(tableName)
        }

        // Try to find cached metadata
        var metaData: ITableMetaData = _tableMap.get(tableName) as ITableMetaData
        if (metaData != null) {
            return metaData
        }

        // Create metadata and cache it
        metaData = DatabaseTableMetaData(tableName, connection, true, super.isCaseSensitiveTableNames)
        // Put the metadata object into the cache map
        _tableMap.update(tableName, metaData)
        return metaData
    }

    @Throws(DataSetException::class)
    fun getTable(tableName: String?): ITable {
        logger.debug("getTable(tableName={}) - start", tableName)
        val qualifiedTableName = QualifiedTableName(tableName, defaultSchema)
        initialize(qualifiedTableName.getSchema())
        return try {
            val metaData: ITableMetaData = getTableMetaData(tableName)
            val config = _connection.config
            val factory = config.getProperty(
                DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY
            ) as IResultSetTableFactory
            factory.createTable(metaData, _connection)
        } catch (e: SQLException) {
            throw DataSetException(e)
        }
    }

    private class SchemaSet constructor(private val isCaseSensitive: Boolean) : HashSet<String?>() {
        override operator fun contains(element: String?): Boolean {
            return super.contains(normalizeSchema(element))
        }

        override fun add(e: String?): Boolean {
            return super.add(normalizeSchema(e))
        }

        private fun normalizeSchema(source: Any?): String {
            if (source == null) {
                return NULL_REPLACEMENT
            } else if (!isCaseSensitive) {
                return source.toString().uppercase()
            }
            return source.toString()
        }

        companion object {
            private const val serialVersionUID = 1L
            private const val NULL_REPLACEMENT = "NULL_REPLACEMENT_HASHKEY"
        }
    }

    companion object {
        /**
         * Logger for this class
         */
        private val logger = LoggerFactory.getLogger(DatabaseDataSet::class.java)
        @Throws(DataSetException::class)
        fun getSelectStatement(schema: String, metaData: ITableMetaData, escapePattern: String): String {
            if (logger.isDebugEnabled) {
                logger.debug(
                    "getSelectStatement(schema={}, metaData={}, escapePattern={}) - start",
                    *arrayOf<Any>(schema, metaData, escapePattern)
                )
            }
            val columns = metaData.columns
            val primaryKeys = metaData.primaryKeys
            if (columns.size == 0) {
                throw DatabaseUnitRuntimeException(
                    "At least one column is required to build a valid select statement. " +
                            "Cannot load data for " + metaData
                )
            }

            // select
            val sqlBuffer = StringBuffer(128)
            sqlBuffer.append("select ")
            for (i in columns.indices) {
                if (i > 0) {
                    sqlBuffer.append(", ")
                }
                val columnName: String = QualifiedTableName(
                    columns[i].columnName, null
                ).qualifiedTableName()
                sqlBuffer.append(columnName)
            }

            // from
            sqlBuffer.append(" from ")
            sqlBuffer.append(
                QualifiedTableName(
                    metaData.tableName, schema
                ).qualifiedTableName()
            )

            // order by
            for (i in primaryKeys.indices) {
                if (i == 0) {
                    sqlBuffer.append(" order by ")
                } else {
                    sqlBuffer.append(", ")
                }
                sqlBuffer.append(QualifiedTableName(primaryKeys[i].columnName, null)
                    .qualifiedTableName())
            }
            return sqlBuffer.toString()
        }
    }*/
    override fun createIterator(reversed: Boolean): ITableIterator {
        TODO("Not yet implemented")
    }
}
