package org.testaco.dataset.datatype

import org.testaco.dataset.ITable
import org.slf4j.LoggerFactory
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class UnknownDataType : AbstractDataType<Any>("UNKNOWN", Types.OTHER, Any::class, false, false) {
    override fun typeCast(value: Any): Any? {
        logger.debug("typeCast(value={}) - start", value)
        return if (value === ITable.NO_VALUE) {
            null
        } else {
            value
        }
    }

    override fun getSqlValue(column: Int, resultSet: ResultSet?): Any? {
        TODO("Not yet implemented")
    }

    override fun setSqlValue(value: Any?, column: Int, statement: PreparedStatement?) {
        TODO("Not yet implemented")
    }

    companion object {
        /**
         * Logger for this class
         */
        private val logger = LoggerFactory.getLogger(UnknownDataType::class.java)
    }
}
