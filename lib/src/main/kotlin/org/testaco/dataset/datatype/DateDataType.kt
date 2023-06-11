package org.testaco.dataset.datatype

import org.testaco.dataset.ITable
import org.slf4j.LoggerFactory
import java.sql.*
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class DateDataType internal constructor() :
    AbstractDataType<Date>("DATE", Types.DATE, Date::class, false, true) {

    fun typeCast(value: Any?): Any? {
        logger.debug("typeCast(value={}) - start", value)
        if (value == null || value === ITable.NO_VALUE) {
            return null
        }
        if (value is Date) {
            return value
        }
        if (value is java.util.Date) {
            return Date(value.time)
        }
        if (value is Long) {
            return Date(value)
        }
        if (value is String) {
            if (DataType.isExtendedSyntax(value)) {
                // Relative date.
                return try {
                    val datetime: LocalDateTime? = DataType.RELATIVE_DATE_TIME_PARSER.parse(value)
                    Date.valueOf(datetime?.toLocalDate())
                } catch (e: IllegalArgumentException) {
                    throw TypeCastException(value, this, e)
                } catch (e: DateTimeParseException) {
                    throw TypeCastException(value, this, e)
                }
            }

            // Probably a Timestamp, try it just in case!
            if (value.length > 10) {
                try {
                    val time = Timestamp.valueOf(value).time
                    return Date(time)
                    //                    return java.sql.Date.valueOf(new java.sql.Date(time).toString());
                } catch (e: IllegalArgumentException) {
                    // Was not a Timestamp, let java.sql.Date handle this value
                }
            }
            return try {
                Date.valueOf(value)
            } catch (e: IllegalArgumentException) {
                throw TypeCastException(value, this, e)
            }
        }
        throw TypeCastException(value, this)
    }

    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): Date? {
        if (logger.isDebugEnabled) logger.debug("getSqlValue(column={}, resultSet={}) - start", column, resultSet)
        val value = resultSet.getDate(column)
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
        statement.setDate(column, typeCast(value) as Date?)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DateDataType::class.java)
    }
}
