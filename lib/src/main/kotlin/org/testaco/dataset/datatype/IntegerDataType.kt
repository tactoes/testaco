package org.testaco.dataset.datatype

import org.testaco.dataset.ITable
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class IntegerDataType internal constructor(name: String, sqlType: Int) :
    AbstractDataType<Int>(name, sqlType, Int::class, true, false) {

    fun typeCast(value: Any?): Any? {
        logger.debug("typeCast(value={}) - start", value)
        if (value == null || value === ITable.NO_VALUE) {
            return null
        }
        if (value is Number) {
            return value.toInt()
        }

        // Treat "false" as 0, "true" as 1
        if (value is String) {
            val string = value
            if ("false".equals(string, ignoreCase = true)) {
                return 0
            }
            if ("true".equals(string, ignoreCase = true)) {
                return 1
            }
        }

        val stringValue = value.toString().trim { it <= ' ' }
        return if (stringValue.length <= 0) {
            null
        } else try {
            typeCast(BigDecimal(stringValue))
        } catch (e: NumberFormatException) {
            throw TypeCastException(value, this, e)
        }
    }

    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): Int? {
        if (logger.isDebugEnabled) logger.debug("getSqlValue(column={}, resultSet={}) - start", column, resultSet)
        val value = resultSet.getInt(column)
        return if (resultSet.wasNull()) {
            null
        } else {
            value
        }
    }

    @Throws(SQLException::class)
    override fun setSqlValue(value: Any, column: Int, statement: PreparedStatement) {
        if (logger.isDebugEnabled) logger.debug(
            "setSqlValue(value={}, column={}, statement={}) - start",
            *arrayOf(value, column, statement)
        )
        statement.setInt(column, (typeCast(value) as Int?)!!)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IntegerDataType::class.java)
    }
}
