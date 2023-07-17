package org.testaco.database

import org.slf4j.LoggerFactory
import org.testaco.dataset.AbstractTableMetaData
import org.testaco.dataset.Column
import org.testaco.dataset.Columns
import org.testaco.dataset.datatype.IDataTypeFactory
import org.testaco.dataset.exceptions.DataSetException
import org.testaco.dataset.exceptions.NoSuchTableException
import org.testaco.dataset.filter.IColumnFilter
import org.testaco.util.QualifiedTableName
import org.testaco.util.SQLHelper
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class DatabaseTableMetaData @JvmOverloads internal constructor(
    override val tableName: String,
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

    private val _connection: IDatabaseConnection
    private var _columns: List<Column>? = null
    private var _primaryKeys: List<Column>? = null
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

            // qualified names support - table name and schema is stored here
            _qualifiedTableNameSupport = QualifiedTableName(this.tableName, _connection.schema)
            if (validate) {
                val schemaName: String = _qualifiedTableNameSupport!!.schema!!
                val plainTableName: String = _qualifiedTableNameSupport!!.table
                logger.debug("Validating if table '{}' exists in schema '{}' ...", plainTableName, schemaName)
                try {
                    val config = connection.config
                    val metadataHandler =
                        config!!.metaDataHandler
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
    private val primaryKeyNames: List<String>
        private get() {
            logger.debug("getPrimaryKeyNames() - start")
            val schemaName: String = _qualifiedTableNameSupport!!.schema!!
            val tableName: String = _qualifiedTableNameSupport!!.table
            val connection = _connection.connection
            val databaseMetaData: DatabaseMetaData = connection!!.metaData
            val config = _connection.config
            val metadataHandler = config!!.metaDataHandler
            val resultSet: ResultSet? = metadataHandler!!.getPrimaryKeys(databaseMetaData, schemaName, tableName)
            val list = ArrayList<PrimaryKeyData>()
            try {
                while (resultSet!!.next()) {
                    val name: String = resultSet!!.getString(4)
                    val sequence: Int = resultSet!!.getInt(5)
                    list.add(PrimaryKeyData(name, sequence))
                }
            } finally {
                resultSet!!.close()
            }
            Collections.sort(list)
            return list.map { it.name }
        }

    private inner class PrimaryKeyData(val name: String, val index: Int) : Comparable<PrimaryKeyData> {

        //TODO completely bogus implementation
        override fun compareTo(o: PrimaryKeyData): Int {
            return index - o.index
        }
    }

    override val columns: List<Column>
        get() {
            logger.debug("getColumns() - start")
            if (_columns == null) {
                _columns = try {
                    // qualified names support
                    val schemaName: String = _qualifiedTableNameSupport!!.schema!!
                    val tableName: String = _qualifiedTableNameSupport!!.table
                    val jdbcConnection = _connection.connection
                    val databaseMetaData: DatabaseMetaData = jdbcConnection!!.metaData
                    val config = _connection.config
                    val metadataHandler =
                        config!!.metaDataHandler
                    val resultSet: ResultSet? = metadataHandler!!.getColumns(databaseMetaData, schemaName, tableName)
                    try {
                        val dataTypeFactory: IDataTypeFactory = super.getDataTypeFactory(_connection)
                        val datatypeWarning: Boolean = config!!.datatypeWarning
                        val columnList = ArrayList<Column>()
                        while (resultSet!!.next()) {
                            // Check for exact table/schema name match because
                            // databaseMetaData.getColumns() uses patterns for the lookup
                            val match =
                                metadataHandler.matches(resultSet, "", schemaName, tableName, column = "", caseSensitive = _caseSensitiveMetaData)
                            if (match) {
                                val column: Column = SQLHelper.createColumn(resultSet, dataTypeFactory, datatypeWarning)!!
                                columnList.add(column)
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
                        columnList
                    } finally {
                        resultSet!!.close()
                    }
                } catch (e: SQLException) {
                    throw DataSetException("Caught sql exception: "+e.message, e)
                }
            }
            return _columns!!
        }

    private fun primaryKeyFilterChanged(keyFilter: IColumnFilter?): Boolean {
        return keyFilter !== lastKeyFilter
    }

    @get:Throws(DataSetException::class)
    override val primaryKeys: List<Column>
        get() {
            logger.debug("getPrimaryKeys() - start")
            val config = _connection.config
            val primaryKeysFilter: IColumnFilter? = null //TODO: Implement primary key filter
            if (_primaryKeys == null || primaryKeyFilterChanged(primaryKeysFilter)) {
                try {
                    lastKeyFilter = primaryKeysFilter
                    _primaryKeys = if (primaryKeysFilter != null) {
                        Columns.getColumns(
                            tableName, columns,
                            primaryKeysFilter
                        ).toList()
                    } else {
                        val pkNames = primaryKeyNames
                        Columns.getColumns(pkNames, columns)
                    }
                } catch (e: SQLException) {
                    throw DataSetException("Caught sql exception: "+e.message, e)
                }
            }
            return _primaryKeys as List<Column>
        }

    override fun toString(): String {
        return try {
            val tableName = tableName
            val columns = _columns.toString()
            val primaryKeys = _primaryKeys.toString()
            "table=$tableName, cols=$columns, pk=$primaryKeys"
        } catch (e: DataSetException) {
            super.toString()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DatabaseTableMetaData::class.java)
    }
}
