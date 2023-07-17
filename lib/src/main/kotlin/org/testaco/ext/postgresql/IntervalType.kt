package org.testaco.ext.postgresql

import org.postgresql.util.PGInterval
import org.testaco.dataset.datatype.AbstractDataType
import org.testaco.dataset.datatype.TypeCastException
import java.sql.*

class IntervalType : AbstractDataType<String>("interval", Types.OTHER, String::class, false, false) {
    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): String? {
        val s = resultSet.getString(column)
        return if (resultSet.wasNull()) {
            null
        } else {
            s
        }
    }

    @Throws(SQLException::class)
    override fun setSqlValue(
        value: Any, column: Int,
        statement: PreparedStatement
    ) {
        statement.setObject(column, getInterval(value))
    }

    override fun typeCast(value: Any): Any {
        return value.toString()
    }

    @Throws(TypeCastException::class)
    private fun getInterval(value: Any): Any {
        return PGInterval(value.toString())
    }
}