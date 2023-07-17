package org.testaco.dataset.datatype

import org.testaco.dataset.ITable
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.sql.*
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.*
import java.util.Date
import java.util.regex.Pattern

class TimestampDataType internal constructor() :
    AbstractDataType<Timestamp>("TIMESTAMP", Types.TIMESTAMP, Timestamp::class, false, true) {

    override fun typeCast(value: Any): Any? {
        logger.debug("typeCast(value={}) - start", value)
        if (value == null) {
            return null
        }
        if (value is Timestamp) {
            return value
        }
        if (value is Date) {
            return Timestamp(value.time)
        }
        if (value is Long) {
            return Timestamp(value)
        }
        if (value is String) {
            var stringValue = value.toString()
            if (DataType.isExtendedSyntax(stringValue)) {
                // Relative date.
                return try {
                    val datetime: LocalDateTime? = DataType.RELATIVE_DATE_TIME_PARSER.parse(stringValue)
                    Timestamp.valueOf(datetime)
                } catch (e: IllegalArgumentException) {
                    throw TypeCastException(value, this, e)
                } catch (e: DateTimeParseException) {
                    throw TypeCastException(value, this, e)
                }
            }
            var zoneValue: String? = null
            val tzMatcher = TIMEZONE_REGEX.matcher(stringValue)
            if (tzMatcher.matches() && tzMatcher.group(2) != null) {
                stringValue = tzMatcher.group(1)
                zoneValue = tzMatcher.group(2)
            }
            var ts: Timestamp? = null
            if (stringValue.length == 10) {
                try {
                    val time = java.sql.Date.valueOf(stringValue).time
                    ts = Timestamp(time)
                } catch (e: IllegalArgumentException) {
                    // Was not a java.sql.Date, let Timestamp handle this value
                }
            }
            if (ts == null) {
                ts = try {
                    Timestamp.valueOf(stringValue)
                } catch (e: IllegalArgumentException) {
                    throw TypeCastException(value, this, e)
                }
            }

            // Apply zone if any
            if (zoneValue != null) {
                val tsTime = ts!!.time
                val localTZ = TimeZone.getDefault()
                val offset = localTZ.getOffset(tsTime)
                val localTZOffset = BigInteger.valueOf(offset.toLong())
                var time = BigInteger.valueOf(tsTime / 1000 * 1000).add(localTZOffset)
                    .multiply(ONE_BILLION).add(BigInteger.valueOf(ts.nanos.toLong()))
                val hours = zoneValue.substring(1, 3).toInt()
                val minutes = zoneValue.substring(3, 5).toInt()
                val offsetAsSeconds = BigInteger.valueOf((hours * 3600 + minutes * 60).toLong())
                val offsetAsNanos = offsetAsSeconds.multiply(BigInteger.valueOf(1000)).multiply(ONE_BILLION)
                time = if (zoneValue[0] == '+') {
                    time.subtract(offsetAsNanos)
                } else {
                    time.add(offsetAsNanos)
                }
                val components = time.divideAndRemainder(ONE_BILLION)
                ts = Timestamp(components[0].toLong())
                ts.nanos = components[1].toInt()
            }
            return ts
        }
        throw TypeCastException(value, this)
    }

    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): Timestamp? {
        if (logger.isDebugEnabled) {
            logger.debug("getSqlValue(column={}, resultSet={}) - start", column, resultSet)
        }
        val value = resultSet.getTimestamp(column)
        return if (value == null || resultSet.wasNull()) {
            null
        } else value
    }

    @Throws(SQLException::class, TypeCastException::class)
    override fun setSqlValue(value: Any, column: Int, statement: PreparedStatement) {
        if (logger.isDebugEnabled) {
            logger.debug(
                "setSqlValue(value={}, column={}, statement={}) - start",
                *arrayOf(value, column, statement)
            )
        }
        statement.setTimestamp(column, typeCast(value) as Timestamp?)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TimestampDataType::class.java)

        private val ONE_BILLION = BigInteger("1000000000")
        private val TIMEZONE_REGEX = Pattern.compile("(.*)(?:\\W([+-][0-2][0-9][0-5][0-9]))")
    }
}
