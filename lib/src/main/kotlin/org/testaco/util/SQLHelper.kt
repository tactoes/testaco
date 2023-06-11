package org.testaco.util

import org.slf4j.LoggerFactory
import org.testaco.DatabaseUnitRuntimeException
import org.testaco.dataset.Column
import org.testaco.dataset.datatype.DataType
import org.testaco.dataset.datatype.DataTypeException
import org.testaco.dataset.datatype.IDataTypeFactory
import java.io.PrintStream
import java.sql.*

object SQLHelper {
    const val DB_PRODUCT_SYBASE = "Sybase"

    private val logger = LoggerFactory.getLogger(SQLHelper::class.java)

    @Throws(SQLException::class)
    fun getPrimaryKeyColumn(conn: Connection, table: String?): String {
        logger.debug("getPrimaryKeyColumn(conn={}, table={}) - start", conn, table)
        val metadata: DatabaseMetaData = conn.metaData
        val rs: ResultSet = metadata.getPrimaryKeys(null, null, table)
        rs.next()
        return rs.getString(4)
    }

    @Throws(SQLException::class)
    fun close(rs: ResultSet?, stmt: Statement?) {
        logger.debug("close(rs={}, stmt={}) - start", rs, stmt)
        try {
            close(rs)
        } finally {
            close(stmt)
        }
    }

    @Throws(SQLException::class)
    fun close(stmt: Statement?) {
        logger.debug("close(stmt={}) - start", stmt)
        stmt?.close()
    }

    @Throws(SQLException::class)
    fun close(resultSet: ResultSet?) {
        logger.debug("close(resultSet={}) - start", resultSet)
        if (resultSet != null) {
            resultSet.close()
        }
    }

    @Throws(SQLException::class)
    fun schemaExists(connection: Connection, schema: String): Boolean {
        logger.trace("schemaExists(connection={}, schema={}) - start", connection, schema)
        val metaData: DatabaseMetaData = connection.metaData
        val rs: ResultSet = metaData.getSchemas() //null, schemaPattern);
        return try {
            while (rs.next()) {
                val foundSchema: String = rs.getString("TABLE_SCHEM")
                if (foundSchema == schema) {
                    return true
                }
            }

            // Especially for MySQL check the catalog
            if (catalogExists(connection, schema)) {
                logger.debug("Found catalog with name {}. Returning true because DB is probably on MySQL", schema)
                return true
            }
            false
        } finally {
            rs.close()
        }
    }

    @Throws(SQLException::class)
    private fun catalogExists(connection: Connection, catalog: String?): Boolean {
        logger.trace("catalogExists(connection={}, catalog={}) - start", connection, catalog)
        if (catalog == null) {
            throw NullPointerException("The parameter 'catalog' must not be null")
        }
        val metaData: DatabaseMetaData = connection.metaData
        val rs: ResultSet = metaData.getCatalogs()
        return try {
            while (rs.next()) {
                val foundCatalog: String = rs.getString("TABLE_CAT")
                if (foundCatalog == catalog) {
                    return true
                }
            }
            false
        } finally {
            rs.close()
        }
    }

    @Throws(SQLException::class)
    fun printAllTables(metaData: DatabaseMetaData, outputStream: PrintStream) {
        val rs: ResultSet = metaData.getTables(null, null, null, null)
        try {
            while (rs.next()) {
                val catalog: String = rs.getString("TABLE_CAT")
                val schema: String = rs.getString("TABLE_SCHEM")
                val table: String = rs.getString("TABLE_NAME")
                val tableInfo = StringBuffer()
                tableInfo.append(catalog).append(".")
                tableInfo.append(schema).append(".")
                tableInfo.append(table)
                // Print the info
                outputStream.println(tableInfo)
            }
            outputStream.flush()
        } finally {
            close(rs)
        }
    }

    fun getDatabaseInfo(metaData: DatabaseMetaData): String {
        val sb = StringBuffer()
        sb.append("\n")
        var dbInfo: String?
        dbInfo = object : ExceptionWrapper() {
            @Throws(Exception::class)
            override fun wrappedCall(metaData: DatabaseMetaData): String {
                return metaData.getDatabaseProductName()
            }
        }.executeWrappedCall(metaData)
        sb.append("\tdatabase product name=").append(dbInfo).append("\n")
        dbInfo = object : ExceptionWrapper() {
            @Throws(Exception::class)
            override fun wrappedCall(metaData: DatabaseMetaData): String {
                return metaData.getDatabaseProductVersion()
            }
        }.executeWrappedCall(metaData)
        sb.append("\tdatabase version=").append(dbInfo).append("\n")
        dbInfo = object : ExceptionWrapper() {
            @Throws(Exception::class)
            override fun wrappedCall(metaData: DatabaseMetaData): String {
                return metaData.getDatabaseMajorVersion().toString()
            }
        }.executeWrappedCall(metaData)
        sb.append("\tdatabase major version=").append(dbInfo).append("\n")
        dbInfo = object : ExceptionWrapper() {
            @Throws(Exception::class)
            override fun wrappedCall(metaData: DatabaseMetaData): String {
                return metaData.getDatabaseMinorVersion().toString()
            }
        }.executeWrappedCall(metaData)
        sb.append("\tdatabase minor version=").append(dbInfo).append("\n")
        dbInfo = object : ExceptionWrapper() {
            @Throws(Exception::class)
            override fun wrappedCall(metaData: DatabaseMetaData): String {
                return metaData.getDriverName()
            }
        }.executeWrappedCall(metaData)
        sb.append("\tjdbc driver name=").append(dbInfo).append("\n")
        dbInfo = object : ExceptionWrapper() {
            @Throws(Exception::class)
            override fun wrappedCall(metaData: DatabaseMetaData): String {
                return metaData.getDriverVersion()
            }
        }.executeWrappedCall(metaData)
        sb.append("\tjdbc driver version=").append(dbInfo).append("\n")
        dbInfo = object : ExceptionWrapper() {
            @Throws(Exception::class)
            override fun wrappedCall(metaData: DatabaseMetaData): String {
                return metaData.getDriverMajorVersion().toString()
            }
        }.executeWrappedCall(metaData)
        sb.append("\tjdbc driver major version=").append(dbInfo).append("\n")
        dbInfo = object : ExceptionWrapper() {
            @Throws(Exception::class)
            override fun wrappedCall(metaData: DatabaseMetaData): String {
                return metaData.getDriverMinorVersion().toString()
            }
        }.executeWrappedCall(metaData)
        sb.append("\tjdbc driver minor version=").append(dbInfo).append("\n")
        return sb.toString()
    }

    @Throws(SQLException::class)
    fun printDatabaseInfo(metaData: DatabaseMetaData, outputStream: PrintStream) {
        val dbInfo = getDatabaseInfo(metaData)
        try {
            outputStream.println(dbInfo)
        } finally {
            outputStream.flush()
        }
    }

    @Throws(SQLException::class)
    fun isSybaseDb(metaData: DatabaseMetaData): Boolean {
        val dbProductName: String = metaData.getDatabaseProductName()
        return dbProductName == DB_PRODUCT_SYBASE
    }

    @Throws(SQLException::class, DataTypeException::class)
    fun createColumn(
        resultSet: ResultSet,
        dataTypeFactory: IDataTypeFactory, datatypeWarning: Boolean
    ): Column? {
        val tableName: String = resultSet.getString(3)
        val columnName: String = resultSet.getString(4)
        var sqlType: Int = resultSet.getInt(5)
        //If Types.DISTINCT like SQL DOMAIN, then get Source Date Type of SQL-DOMAIN
        if (sqlType == Types.DISTINCT) {
            sqlType = resultSet.getInt("SOURCE_DATA_TYPE")
        }
        val sqlTypeName: String = resultSet.getString(6)
        //        int columnSize = resultSet.getInt(7);
        val nullable: Int = resultSet.getInt(11)
        val remarks: String = resultSet.getString(12)
        val columnDefaultValue: String = resultSet.getString(13)
        // This is only available since Java 5 - so we can try it and if it does not work default it
        var isAutoIncrement: String = Column.AutoIncrement.NO.key
        try {
            isAutoIncrement = resultSet.getString(23)
        } catch (e: Exception) {
            // Ignore this one here
            val msg = ("Could not retrieve the 'isAutoIncrement' property"
                    + " because not yet running on Java 1.5 -"
                    + " defaulting to NO. Table={}, Column={}")
            logger.debug(msg, tableName, columnName, e)
        }

        // Convert SQL type to DataType
        val dataType: DataType<*> = dataTypeFactory.createDataType(sqlType, sqlTypeName, tableName, columnName)
        return if (dataType !== DataType.UNKNOWN) {
            Column(
                columnName, dataType,
                sqlTypeName, Column.nullableValue(nullable), columnDefaultValue, remarks,
                Column.AutoIncrement.autoIncrementValue(isAutoIncrement)
            )
        } else {
            if (datatypeWarning) logger.warn(
                tableName + "." + columnName +
                        " data type (" + sqlType + ", '" + sqlTypeName +
                        "') not recognized and will be ignored. See FAQ for more information."
            )

            // datatype unknown - column not created
            null
        }
    }

    /**
     * Compares the given values and returns true if they are equal.
     * If the first value is `null` or empty String it always
     * returns `true` which is the way of ignoring `null`s
     * for this specific case.
     * @param value1 The first value to compare. Is ignored if null or empty String
     * @param value2 The second value to be compared
     * @return `true` if both values are equal or if the first value
     * is `null` or empty string.
     * @since 2.4.4
     */
    fun areEqualIgnoreNull(value1: String?, value2: String?, caseSensitive: Boolean): Boolean {
        return if (value1 == null || value1 == "") {
            true
        } else {
            if (caseSensitive && value1 == value2) {
                true
            } else if (!caseSensitive && value1.equals(value2, ignoreCase = true)) {
                true
            } else {
                false
            }
        }
    }

    /**
     * Corrects the case of the given String according to the way in which the database stores metadata.
     * @param databaseIdentifier A database identifier such as a table name or a schema name for
     * which the case should be corrected.
     * @param connection The connection used to lookup the database metadata. This is needed to determine
     * the way in which the database stores its metadata.
     * @return The database identifier in the correct case for the RDBMS
     * @since 2.4.4
     */
    fun correctCase(databaseIdentifier: String, connection: Connection): String {
        logger.trace("correctCase(tableName={}, connection={}) - start", databaseIdentifier, connection)
        return try {
            correctCase(databaseIdentifier, connection.metaData)
        } catch (e: SQLException) {
            throw DatabaseUnitRuntimeException("Exception while trying to access database metadata", e)
        }
    }

    /**
     * Corrects the case of the given String according to the way in which the database stores metadata.
     * @return The database identifier in the correct case for the RDBMS
     * @since 2.4.4
     */
    fun correctCase(databaseIdentifier: String, databaseMetaData: DatabaseMetaData): String {
        logger.trace("correctCase(tableName={}, databaseMetaData={}) - start", databaseIdentifier, databaseMetaData)
        return try {
            var resultTableName: String = databaseIdentifier
            val dbIdentifierQuoteString: String = databaseMetaData.getIdentifierQuoteString()
            if (!isEscaped(databaseIdentifier, dbIdentifierQuoteString)) {
                if (databaseMetaData.storesLowerCaseIdentifiers()) {
                    resultTableName = databaseIdentifier.lowercase()
                } else if (databaseMetaData.storesUpperCaseIdentifiers()) {
                    resultTableName = databaseIdentifier.uppercase()
                } else {
                    logger.debug(
                        "Database does not store upperCase or lowerCase identifiers. " +
                                "Will not correct case of the table names."
                    )
                }
            } else {
                if (logger.isDebugEnabled) logger.debug(
                    "The tableName '{}' is escaped. Will not correct case.",
                    databaseIdentifier
                )
            }
            resultTableName
        } catch (e: SQLException) {
            throw DatabaseUnitRuntimeException("Exception while trying to access database metadata", e)
        }
    }

    /**
     * Checks whether two given values are unequal and if so print a log message (level INFO)
     */
    fun logInfoIfValueChanged(oldValue: String?, newValue: String, message: String, source: Class<*>) {
        if (logger.isInfoEnabled) {
            if (oldValue != null && oldValue != newValue) logger.info(
                "{}. {} oldValue={} newValue={}",
                *arrayOf<Any>(source, message, oldValue, newValue)
            )
        }
    }

    /**
     * Checks whether two given values are unequal and if so print a log message (level DEBUG)
     */
    fun logDebugIfValueChanged(oldValue: String?, newValue: String, message: String, source: Class<*>) {
        if (logger.isDebugEnabled) {
            if (oldValue != null && oldValue != newValue) logger.debug(
                "{}. {} oldValue={} newValue={}",
                *arrayOf<Any>(source, message, oldValue, newValue)
            )
        }
    }

    private fun isEscaped(tableName: String, dbIdentifierQuoteString: String): Boolean {
        logger.trace("isEscaped(tableName={}, dbIdentifierQuoteString={}) - start", tableName, dbIdentifierQuoteString)
        val isEscaped = tableName.startsWith(dbIdentifierQuoteString)
        if (logger.isDebugEnabled) logger.debug(
            "isEscaped returns '{}' for tableName={} (dbIdentifierQuoteString={})",
            *arrayOf<Any?>(java.lang.Boolean.valueOf(isEscaped), tableName, dbIdentifierQuoteString)
        )
        return isEscaped
    }

    /**
     * Performs a method invocation and catches all exceptions that occur during the invocation.
     * Utility which works similar to a closure, just a bit less elegant.
     * @author gommma (gommma AT users.sourceforge.net)
     * @author Last changed by: $Author$
     * @version $Revision$ $Date$
     * @since 2.4.6
     */
    internal abstract class ExceptionWrapper
    /**
     * Default constructor
     */
    {
        /**
         * Executes the call and catches all exception that might occur.
         * @param metaData
         * @return The result of the call
         */
        fun executeWrappedCall(metaData: DatabaseMetaData): String {
            return try {
                wrappedCall(metaData)
            } catch (e: Exception) {
                logger.trace("Problem retrieving DB information via DatabaseMetaData", e)
                NOT_AVAILABLE_TEXT
            }
        }

        /**
         * Calls the method that might throw an exception to be handled
         * @param metaData
         * @return The result of the call as human readable string
         * @throws Exception Any exception that might occur during the method invocation
         */
        @Throws(Exception::class)
        abstract fun wrappedCall(metaData: DatabaseMetaData): String

        companion object {
            const val NOT_AVAILABLE_TEXT = "<not available>"
        }
    }
}
