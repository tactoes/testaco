package org.testaco.dataset.datatype

import org.testaco.dataset.ITable
import org.slf4j.LoggerFactory
import java.sql.*
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.*
import java.util.Date

class TimeDataType internal constructor() :
    AbstractDataType<Time>("TIME", Types.TIME, Time::class, false, true) {

    fun typeCast(value: Any?): Any? {
        logger.debug("typeCast(value={}) - start", value)
        if (value == null || value === ITable.NO_VALUE) {
            return null
        }
        if (value is Time) {
            return value
        }
        if (value is Date) {
            return Time(value.time)
        }
        if (value is Long) {
            return Time(value)
        }
        if (value is String) {
            val stringValue = value
            return if (DataType.isExtendedSyntax(stringValue)) {
                // Relative date.
                try {
                    val datetime: LocalDateTime? = DataType.RELATIVE_DATE_TIME_PARSER.parse(stringValue)
                    Time.valueOf(datetime?.toLocalTime())
                } catch (e: IllegalArgumentException) {
                    throw TypeCastException(value, this, e)
                } catch (e: DateTimeParseException) {
                    throw TypeCastException(value, this, e)
                }
            } else try {
                Time.valueOf(stringValue)
            } catch (e: IllegalArgumentException) {
                throw TypeCastException(value, this, e)
            }
        }
        throw TypeCastException(value, this)
    }

    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): Time? {
        if (logger.isDebugEnabled) logger.debug("getSqlValue(column={}, resultSet={}) - start", column, resultSet)
        val value = resultSet.getTime(column)
        return if (value == null || resultSet.wasNull()) {
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
        statement.setTime(column, typeCast(value) as Time?)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TimeDataType::class.java)
    }
}
