package org.testaco.dataset.datatype

import org.testaco.dataset.ITable
import org.slf4j.LoggerFactory
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

open class BooleanDataType @JvmOverloads internal constructor(name: String = "BOOLEAN", sqlType: Int = Types.BOOLEAN) :
    AbstractDataType<Boolean>(name, sqlType, Boolean::class, false, false) {

    fun typeCast(value: Any?): Any? {
        logger.debug("typeCast(value={}) - start", value)
        if (value == null || value === ITable.NO_VALUE) {
            return null
        }
        if (value is Boolean) {
            return value
        }
        if (value is Number) {
            return if (value.toInt() == 0) java.lang.Boolean.FALSE else java.lang.Boolean.TRUE
        }
        if (value is String) {
            val string = value
            return if (string.equals("true", ignoreCase = true) || string.equals("false", ignoreCase = true)) {
                java.lang.Boolean.valueOf(string)
            } else {
                typeCast(DataType.INTEGER.typeCast(string))
            }
        }
        throw TypeCastException(value, this)
    }

    protected fun compareNonNulls(value1: Any, value2: Any): Int {
        logger.debug("compareNonNulls(value1={}, value2={}) - start", value1, value2)
        val value1bool = value1 as Boolean
        val value2bool = value2 as Boolean
        return value1bool.compareTo(value2bool)
    }

    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): Boolean? {
        if (logger.isDebugEnabled) logger.debug("getSqlValue(column={}, resultSet={}) - start", column, resultSet)
        val value: Boolean = resultSet.getBoolean(column)
        if (resultSet.wasNull()) {
            return null
        } else {
            return value
        }
    }

    @Throws(SQLException::class)
    override fun setSqlValue(value: Any, column: Int, statement: PreparedStatement) {
        if (logger.isDebugEnabled) logger.debug(
            "setSqlValue(value={}, column={}, statement={}) - start",
            *arrayOf(value, column, statement)
        )
        val castValue = typeCast(value) as Boolean?
        if (castValue == null) {
            statement.setNull(column, Types.BOOLEAN)
        } else {
            statement.setBoolean(column, castValue)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BooleanDataType::class.java)
    }
}
