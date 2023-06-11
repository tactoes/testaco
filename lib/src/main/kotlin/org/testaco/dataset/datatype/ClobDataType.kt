package org.testaco.dataset.datatype

import org.slf4j.LoggerFactory
import java.sql.*

class ClobDataType : StringDataType("CLOB", Types.CLOB) {
    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): String? {
        if (logger.isDebugEnabled) logger.debug("getSqlValue(column={}, resultSet={}) - start", column, resultSet)
        val value = resultSet.getClob(column)
        return if (resultSet.wasNull()) {
            null
        } else
            value.getCharacterStream().use { it.readText() }
    }

    @Throws(SQLException::class)
    override fun setSqlValue(value: Any, column: Int, statement: PreparedStatement) {
        if (logger.isDebugEnabled) logger.debug(
            "setSqlValue(value={}, column={}, statement={}) - start",
            *arrayOf(value, column, statement)
        )
        statement.setObject(column, typeCast(value), sqlType)
    }

    companion object {
        /**
         * Logger for this class
         */
        private val logger = LoggerFactory.getLogger(ClobDataType::class.java)
    }
}
