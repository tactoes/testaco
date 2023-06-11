package org.testaco.database

import org.slf4j.LoggerFactory
import org.testaco.dataset.AbstractTableMetaData
import java.util.*

class DatabaseTableMetaData @JvmOverloads internal constructor(
    tableName: String?,
    connection: IDatabaseConnection?,
    validate: Boolean = true,
    caseSensitiveMetaData: Boolean = false
) : AbstractTableMetaData() {
    /**
     * Table name, potentially qualified
     */
    private var _qualifiedTableNameSupport: QualifiedTableName? =
        null// Ensure that the same table name is returned as specified in the input.

    // This is necessary to support fully qualified XML dataset imports.
    //"<dataset>"
    //"<FREJA.SALES SALES_ID=\"8756\" DEALER_ID=\"4467\"/>"
    //"<CAS.ORDERS ORDER_ID=\"1000\" DEALER_CODE=\"4468\"/>"
    //"</dataset>";

    val tableName: String? = null
    private val _connection: IDatabaseConnection
    private var _columns: Array<Column>?
    private var _primaryKeys: Array<Column>?
    private val _caseSensitiveMetaData: Boolean

    //added by hzhan032
    private var lastKeyFilter: IColumnFilter? = null

    init {
        if (tableName == null) {
            throw NullPointerException("The parameter 'tableName' must not be null")
        }
        if (connection == null) {
            throw NullPointerException("The parameter 'connection' must not be null")
        }
        _connection = connection
        _caseSensitiveMetaData = caseSensitiveMetaData
        try {
            val jdbcConnection = connection.connection
            if (!caseSensitiveMetaData) {
                this.tableName = SQLHelper.correctCase(tableName, jdbcConnection)
                SQLHelper.logDebugIfValueChanged(
                    tableName,
                    this.tableName,
                    "Corrected table name:",
                    DatabaseTableMetaData::class.java
                )
            } else {
                this.tableName = tableName
            }

            // qualified names support - table name and schema is stored here
            _qualifiedTableNameSupport = QualifiedTableName(this.tableName, _connection.schema)
            if (validate) {
                val schemaName: String = _qualifiedTableNameSupport.getSchema()
                val plainTableName: String = _qualifiedTableNameSupport.getTable()
                logger.debug("Validating if table '{}' exists in schema '{}' ...", plainTableName, schemaName)
                try {
                    val config = connection.config
                    val metadataHandler =
                        config!!.getProperty(DatabaseConfig.PROPERTY_METADATA_HANDLER) as IMetadataHandler?
                    val databaseMetaData: DatabaseMetaData = jdbcConnection!!.metaData
                    if (!metadataHandler!!.tableExists(databaseMetaData, schemaName, plainTableName)) {
                        throw NoSuchTableException("Did not find table '$plainTableName' in schema '$schemaName'")
                    }
                } catch (e: SQLException) {
                    throw DataSetException("Exception while validation existence of table '$plainTableName'", e)
                }
            } else {
                logger.debug("Validation switched off. Will not check if table exists.")
            }
        } catch (e: SQLException) {
            throw DataSetException(
                "Exception while retrieving JDBC connection from testaco connection '$connection'",
                e
            )
        }
    }

    @get:Throws(SQLException::class)
    private val primaryKeyNames: Array<String?>
        private get() {
            logger.debug("getPrimaryKeyNames() - start")
            val schemaName: String = _qualifiedTableNameSupport.getSchema()
            val tableName: String = _qualifiedTableNameSupport.getTable()
            val connection = _connection.connection
            val databaseMetaData: DatabaseMetaData = connection!!.metaData
            val config = _connection.config
            val metadataHandler = config!!.getProperty(DatabaseConfig.PROPERTY_METADATA_HANDLER) as IMetadataHandler?
            val resultSet: ResultSet? = metadataHandler!!.getPrimaryKeys(databaseMetaData, schemaName, tableName)
            val list: MutableList<*> = ArrayList<Any?>()
            try {
                while (resultSet.next()) {
                    val name: String = resultSet.getString(4)
                    val sequence: Int = resultSet.getInt(5)
                    list.add(PrimaryKeyData(name, sequence))
                }
            } finally {
                resultSet.close()
            }
            Collections.sort<Comparable<*>>(list)
            val keys = arrayOfNulls<String>(list.size)
            for (i in keys.indices) {
                val data = list[i] as PrimaryKeyData
                keys[i] = data.getName()
            }
            return keys
        }

    private inner class PrimaryKeyData(private val _name: String, index: Int) : Comparable<Any?> {
        val index: Int

        init {
            _index = index
        }

        val name: String
            get() {
                logger.debug("getName() - start")
                return _name
            }

        ////////////////////////////////////////////////////////////////////////
        // Comparable interface
        override fun compareTo(o: Any?): Int {
            val data = o as PrimaryKeyData?
            return getIndex() - data.getIndex()
        }
    }

    @get:Throws(DataSetException::class)
    val columns: Array<Any>?
        get() {
            logger.debug("getColumns() - start")
            if (_columns == null) {
                _columns = try {
                    // qualified names support
                    val schemaName: String = _qualifiedTableNameSupport.getSchema()
                    val tableName: String = _qualifiedTableNameSupport.getTable()
                    val jdbcConnection = _connection.connection
                    val databaseMetaData: DatabaseMetaData = jdbcConnection!!.metaData
                    val config = _connection.config
                    val metadataHandler =
                        config!!.getProperty(DatabaseConfig.PROPERTY_METADATA_HANDLER) as IMetadataHandler?
                    val resultSet: ResultSet? = metadataHandler!!.getColumns(databaseMetaData, schemaName, tableName)
                    try {
                        val dataTypeFactory: IDataTypeFactory = super.getDataTypeFactory(_connection)
                        val datatypeWarning: Boolean = config.getFeature(
                            DatabaseConfig.FEATURE_DATATYPE_WARNING
                        )
                        val columnList: MutableList<*> = ArrayList<Any?>()
                        while (resultSet.next()) {
                            // Check for exact table/schema name match because
                            // databaseMetaData.getColumns() uses patterns for the lookup
                            val match =
                                metadataHandler.matches(resultSet, schemaName, tableName, _caseSensitiveMetaData)
                            if (match) {
                                val column: Column = SQLHelper.createColumn(resultSet, dataTypeFactory, datatypeWarning)
                                if (column != null) {
                                    columnList.add(column)
                                }
                            } else {
                                logger.debug(
                                    "Skipping <schema.table> '" + resultSet.getString(2) + "." +
                                            resultSet.getString(3) + "' because names do not exactly match."
                                )
                            }
                        }
                        if (columnList.size == 0) {
                            logger.warn(
                                "No columns found for table '" + tableName + "' that are supported by testaco. " +
                                        "Will return an empty column list"
                            )
                        }
                        columnList.toArray(arrayOfNulls<Column>(0)) as Array<Column>?
                    } finally {
                        resultSet.close()
                    }
                } catch (e: SQLException) {
                    throw DataSetException(e)
                }
            }
            return _columns
        }

    private fun primaryKeyFilterChanged(keyFilter: IColumnFilter?): Boolean {
        return keyFilter !== lastKeyFilter
    }

    @get:Throws(DataSetException::class)
    val primaryKeys: Array<Any>
        get() {
            logger.debug("getPrimaryKeys() - start")
            val config = _connection.config
            val primaryKeysFilter: IColumnFilter? = config!!.getProperty(
                DatabaseConfig.PROPERTY_PRIMARY_KEY_FILTER
            ) as IColumnFilter?
            if (_primaryKeys == null || primaryKeyFilterChanged(primaryKeysFilter)) {
                try {
                    lastKeyFilter = primaryKeysFilter
                    _primaryKeys = if (primaryKeysFilter != null) {
                        Columns.getColumns(
                            tableName, columns,
                            primaryKeysFilter
                        )
                    } else {
                        val pkNames = primaryKeyNames
                        Columns.getColumns(pkNames, columns)
                    }
                } catch (e: SQLException) {
                    throw DataSetException(e)
                }
            }
            return _primaryKeys
        }

    override fun toString(): String {
        return try {
            val tableName = tableName
            val columns = Arrays.asList<Array<Column>?>(*columns).toString()
            val primaryKeys = Arrays.asList<Array<Column>>(*primaryKeys).toString()
            "table=$tableName, cols=$columns, pk=$primaryKeys"
        } catch (e: DataSetException) {
            super.toString()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DatabaseTableMetaData::class.java)
    }
}
