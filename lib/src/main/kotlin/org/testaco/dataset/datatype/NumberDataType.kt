package org.testaco.dataset.datatype

import org.testaco.dataset.ITable
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * @author Manuel Laflamme
 * @version $Revision$
 * @since Feb 17, 2002
 */
open class NumberDataType internal constructor(name: String, sqlType: Int) :
    AbstractDataType<BigDecimal>(name, sqlType, BigDecimal::class, true, false) {

    fun typeCast(value: Any?): Any? {
        logger.debug("typeCast(value={}) - start", value)
        if (value == null || value === ITable.NO_VALUE) {
            return null
        }
        if (value is BigDecimal) {
            return value
        }
        return if (value is Boolean) {
            if (value) TRUE else FALSE
        } else
            BigDecimal(value.toString())
    }

    @Throws(SQLException::class, TypeCastException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): BigDecimal? {
        if (logger.isDebugEnabled) logger.debug("getSqlValue(column={}, resultSet={}) - start", column, resultSet)
        val value = resultSet.getBigDecimal(column)
        return if (resultSet.wasNull()) {
            null
        } else {
            value
        }
    }

    @Throws(SQLException::class, TypeCastException::class)
    override fun setSqlValue(value: Any, column: Int, statement: PreparedStatement) {
        if (logger.isDebugEnabled) logger.debug(
            "setSqlValue(value={}, column={}, statement={}) - start",
            *arrayOf(value, column, statement)
        )
        statement.setBigDecimal(column, typeCast(value) as BigDecimal?)
    }

    companion object {
        /**
         * Logger for this class
         */
        private val logger = LoggerFactory.getLogger(NumberDataType::class.java)
        private val TRUE: Number = BigDecimal(1.0)
        private val FALSE: Number = BigDecimal(0.0)
    }
}
