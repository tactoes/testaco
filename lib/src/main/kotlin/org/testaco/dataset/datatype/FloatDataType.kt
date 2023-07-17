package org.testaco.dataset.datatype

import org.testaco.dataset.ITable
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

class FloatDataType internal constructor() :
    AbstractDataType<Float>("REAL", Types.REAL, Float::class, true, false) {

    override fun typeCast(value: Any): Any? {
        logger.debug("typeCast(value={}) - start", value)
        if (value == null) {
            return null
        }
        return if (value is Number) {
            value.toFloat()
        } else try {
            typeCast(BigDecimal(value.toString()))
        } catch (e: NumberFormatException) {
            throw TypeCastException(value, this, e)
        }
    }

    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): Float? {
        if (logger.isDebugEnabled) logger.debug("getSqlValue(column={}, resultSet={}) - start", column, resultSet)
        val value = resultSet.getFloat(column)
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
        statement.setFloat(column, (typeCast(value) as Number?)!!.toFloat())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FloatDataType::class.java)
    }
}
