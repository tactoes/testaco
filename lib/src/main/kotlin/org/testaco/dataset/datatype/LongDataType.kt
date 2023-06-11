package org.testaco.dataset.datatype

import org.testaco.dataset.ITable
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

class LongDataType internal constructor() : AbstractDataType<Long>("BIGINT", Types.BIGINT, Long::class, true, false) {

    fun typeCast(value: Any?): Any? {
        logger.debug("typeCast(value={}) - start", value)
        if (value == null || value === ITable.NO_VALUE) {
            return null
        }
        return if (value is Number) {
            value.toLong()
        } else try {
            typeCast(BigDecimal(value.toString()))
        } catch (e: NumberFormatException) {
            throw TypeCastException(value, this, e)
        }
    }

    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): Long? {
        if (logger.isDebugEnabled) logger.debug("getSqlValue(column={}, resultSet={}) - start", column, resultSet)
        val value = resultSet.getLong(column)
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
        statement.setLong(column, (typeCast(value) as Number?)!!.toLong())
    }

    companion object {
        /**
         * Logger for this class
         */
        private val logger = LoggerFactory.getLogger(LongDataType::class.java)
    }
}
