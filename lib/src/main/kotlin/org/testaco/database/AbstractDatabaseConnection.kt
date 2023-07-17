package org.testaco.database

import org.slf4j.LoggerFactory
import org.testaco.DatabaseUnitRuntimeException
import org.testaco.database.statement.IStatementFactory
import org.testaco.dataset.IDataSet
import org.testaco.dataset.ITable
import org.testaco.dataset.ITableIterator
import org.testaco.dataset.ITableMetaData
import org.testaco.dataset.exceptions.DataSetException
import org.testaco.util.QualifiedTableName
import org.testaco.util.SQLHelper
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

abstract class AbstractDatabaseConnection(override val config: DatabaseConfig) : IDatabaseConnection {
    private var _dataSet: IDataSet? = null

    @Throws(SQLException::class)
    override fun createDataSet(): IDataSet? {
        logger.debug("createDataSet() - start")
        if (_dataSet == null) {
            _dataSet = DatabaseDataSet(this, false)
        }
        return _dataSet
    }

    @Throws(DataSetException::class, SQLException::class)
    override fun createDataSet(tableNames: Array<String>): IDataSet {
        logger.debug("createDataSet(tableNames={}) - start", *tableNames)
        return object : IDataSet {
            override val tableNames: Array<String>
                get() = TODO("Not yet implemented")

            override fun getTableMetaData(tableName: String): ITableMetaData {
                TODO("Not yet implemented")
            }

            override fun getTable(tableName: String): ITableMetaData {
                TODO("Not yet implemented")
            }

            override fun iterator(): ITableIterator {
                TODO("Not yet implemented")
            }

            override fun reverseIterator(): ITableIterator {
                TODO("Not yet implemented")
            }

            override val isCaseSensitiveTableNames: Boolean
                get() = TODO("Not yet implemented")
        } // FilteredDataSet(tableNames, createDataSet())
    }

    @Throws(DataSetException::class, SQLException::class)
    override fun createQueryTable(tableName: String, sql: String): ITable {
        logger.debug(
            "createQueryTable(resultName={}, sql={}) - start",
            tableName,
            sql,
        )
        val tableFactory = resultSetTableFactory
        val rsTable = tableFactory.createTable(tableName, sql, this)
        if (logger.isDebugEnabled) {
            val rowCount = try {
                val rowCountInt = rsTable.rowCount
                rowCountInt.toString()
            } catch (e: Exception) {
                (
                    "Unable to determine row count due to Exception: " +
                        e.localizedMessage
                    )
            }
            logger.debug("createQueryTable: rowCount={}", rowCount)
        }
        return rsTable
    }

    @Throws(DataSetException::class, SQLException::class)
    override fun createTable(
        tableName: String,
        preparedStatement: PreparedStatement,
    ): ITable {
        logger.debug(
            "createQueryTable(resultName={}, preparedStatement={}) - start",
            tableName,
            preparedStatement,
        )
        val tableFactory = resultSetTableFactory
        return tableFactory.createTable(tableName, preparedStatement, this)
    }

    @Throws(DataSetException::class, SQLException::class)
    override fun createTable(tableName: String): ITable {
        logger.debug("createTable(tableName={}) - start", tableName)
        //        val escapePattern = config.es
        //          .getProperty(DatabaseConfig.PROPERTY_ESCAPE_PATTERN) as String

        // qualify with schema if configured
        val qualifiedTableName = QualifiedTableName(
            tableName,
            schema,
        )
        val qualifiedName: String = qualifiedTableName.qualifiedTableName()
        val sql = "select * from $qualifiedName"
        return createQueryTable(tableName, sql)
    }

    @Throws(SQLException::class)
    override fun getRowCount(tableName: String): Int {
        logger.debug("getRowCount(tableName={}) - start", tableName)
        return getRowCount(tableName, "")
    }

    @Throws(SQLException::class)
    override fun getRowCount(tableName: String, whereClause: String): Int {
        logger.debug(
            "getRowCount(tableName={}, whereClause={}) - start",
            tableName,
            whereClause,
        )
        val sqlBuffer = StringBuffer(128)
        sqlBuffer.append("select count(*) from ")

        // add table name and schema (schema only if available)
        val qualifiedTableName = QualifiedTableName(tableName, schema)
        val qualifiedName: String = qualifiedTableName.qualifiedTableName()
        sqlBuffer.append(qualifiedName)
        sqlBuffer.append(" ")
        sqlBuffer.append(whereClause)
        val statement = connection.createStatement()
        var resultSet: ResultSet? = null
        return try {
            resultSet = statement.executeQuery(sqlBuffer.toString())
            if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                throw DatabaseUnitRuntimeException(
                    "Select count did not return any results for table '" +
                        tableName + "'. Statement: " +
                        sqlBuffer.toString(),
                )
            }
        } finally {
            SQLHelper.close(resultSet, statement)
        }
    }

    @get:Deprecated("Use {@link #getConfig}")
    val statementFactory: IStatementFactory
        get() = config.statementFactory
    private val resultSetTableFactory: IResultSetTableFactory
        get() = config.resultsetFactory

    override fun toString(): String {
        val sb = StringBuffer()
        sb.append("_databaseConfig=").append(config)
        sb.append(", _dataSet=").append(_dataSet)
        return sb.toString()
    }

    companion object {
        /**
         * Logger for this class
         */
        private val logger = LoggerFactory.getLogger(AbstractDatabaseConnection::class.java)
    }
}
