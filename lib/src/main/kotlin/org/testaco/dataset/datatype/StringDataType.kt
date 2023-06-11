package org.testaco.dataset.datatype

import org.testaco.dataset.ITable
import org.slf4j.LoggerFactory
import java.sql.*
import java.time.temporal.TemporalAccessor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * @author Manuel Laflamme
 * @author Last changed by: $Author$
 * @version $Revision$ $Date$
 * @since 1.0
 */
open class StringDataType(name: String, sqlType: Int) :
    AbstractDataType<String>(name, sqlType, String::class, false, false) {

    @OptIn(ExperimentalEncodingApi::class)
    @Throws(TypeCastException::class)
    fun typeCast(value: Any?): Any? {
        logger.debug("typeCast(value={}) - start", value)
        if (value == null || value === ITable.NO_VALUE) {
            return null
        }
        if (value is String) {
            return value
        }
        if (value is Date ||
            value is Time ||
            value is Timestamp ||
            value is TemporalAccessor
        ) {
            return value.toString()
        }
        if (value is Boolean) {
            return value.toString()
        }
        if (value is Number) {
            try {
                return value.toString()
            } catch (e: NumberFormatException) {
                throw TypeCastException(value, this, e)
            }
        }
        if (value is ByteArray) {
            return Base64.Default.encode(value)
        }
        if (value is Blob) {
            try {
                val blob = value
                val blobValue = blob.getBytes(1, blob.length().toInt())
                return typeCast(blobValue)
            } catch (e: SQLException) {
                throw TypeCastException(value, this, e)
            }
        }
        if (value is Clob) {
            try {
                val clobValue = value
                val length = clobValue.length().toInt()
                return if (length > 0) {
                    clobValue.getSubString(1, length)
                } else ""
            } catch (e: SQLException) {
                throw TypeCastException(value, this, e)
            }
        }
        logger.warn(
            "Unknown/unsupported object type '{}' - " +
                    "will invoke toString() as last fallback which " +
                    "might produce undesired results",
            value.javaClass.name
        )
        return value.toString()
    }

    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): String? {
        if (logger.isDebugEnabled) logger.debug("getSqlValue(column={}, resultSet={}) - start", column, resultSet)
        val value = resultSet.getString(column)
        return if (resultSet.wasNull()) {
            null
        } else {
            value
        }
    }

    @Throws(SQLException::class)
    override fun setSqlValue(value: Any, column: Int, statement: PreparedStatement) {
        if (logger.isDebugEnabled)
            logger.debug(
                "setSqlValue(value={}, column={}, statement={}) - start",
                *arrayOf(value, column, statement)
            )
        statement.setString(column, DataType.asString(value))
    }

    companion object {
        /**
         * Logger for this class
         */
        private val logger = LoggerFactory.getLogger(StringDataType::class.java)
    }
}
