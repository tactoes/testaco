package org.testaco.dataset.datatype

import org.slf4j.LoggerFactory
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

class BlobDataType : BytesDataType("BLOB", Types.BLOB) {

    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): ByteArray? {
        if (logger.isDebugEnabled) logger.debug(
            "getSqlValue(column={}, resultSet={}) - start",
            column.toString(),
            resultSet
        )
        val value = resultSet.getBlob(column)
        return if (value == null || resultSet.wasNull()) {
            null
        } else {
            //You better not create database files larger than memory. Not my problem if you do
            value.getBytes(1, value.length().toInt())
        }
    }

    @Throws(SQLException::class, TypeCastException::class)
    override fun setSqlValue(value: Any, column: Int, statement: PreparedStatement) {
        if (logger.isDebugEnabled) {
            logger.debug(
                "setSqlValue(value={}, column={}, statement={}) - start",
                *arrayOf(value, column.toString(), statement)
            )
        }
        statement.setObject(column, typeCast(value), super.sqlType)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BlobDataType::class.java)
    }
}
