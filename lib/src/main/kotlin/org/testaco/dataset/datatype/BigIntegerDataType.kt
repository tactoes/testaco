package org.testaco.dataset.datatype

import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

class BigIntegerDataType : AbstractDataType<BigInteger>("BIGINT", Types.BIGINT, BigInteger::class, true, false) {

    override fun typeCast(value: Any): Any? {
        if (value == null) {
            return null
        }
        when (value) {
            is BigInteger -> {
                return value
            }

            is BigDecimal -> {
                return value.toBigInteger()
            }

            is Number -> {
                val l = value.toLong()
                return BigInteger(l.toString())
            }

            else -> return try {
                val bd = BigDecimal(value.toString())
                bd.toBigInteger()
            } catch (e: NumberFormatException) {
                throw TypeCastException(value, this, e)
            }
        }
    }

    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): BigInteger? {
        if (logger.isDebugEnabled) {
            logger.debug("getSqlValue(column={}, resultSet={}) - start", column, resultSet)
        }
        val value = resultSet.getBigDecimal(column)
        return if (resultSet.wasNull()) {
            null
        } else {
            value.toBigInteger()
        }
    }

    @Throws(SQLException::class)
    override fun setSqlValue(value: Any, column: Int, statement: PreparedStatement) {
        if (logger.isDebugEnabled) {
            logger.debug(
                "setSqlValue(value={}, column={}, statement={}) - start",
                *arrayOf(value, column, statement),
            )
        }
        val valueBigDecimal = (typeCast(value) as BigInteger?)?.let { BigDecimal(it) }
        statement.setBigDecimal(column, valueBigDecimal)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BigIntegerDataType::class.java)
    }
}
