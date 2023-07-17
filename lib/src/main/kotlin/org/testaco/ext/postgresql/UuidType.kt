package org.testaco.ext.postgresql

import org.postgresql.util.PGobject
import org.testaco.dataset.datatype.AbstractDataType
import org.testaco.dataset.datatype.TypeCastException
import java.sql.*

class UuidType : AbstractDataType<String>("uuid", Types.OTHER, String::class, false, false) {
    @Throws(SQLException::class)
    override fun getSqlValue(column: Int, resultSet: ResultSet): String? {
        val s = resultSet.getString(column)
        return if (resultSet.wasNull()) {
            null
        } else {
            s
        }
    }

    @Throws(SQLException::class, TypeCastException::class)
    override fun setSqlValue(
        value: Any, column: Int,
        statement: PreparedStatement
    ) {
        statement.setObject(column, getUUID(value))
    }

    @Throws(TypeCastException::class)
    override fun typeCast(value: Any): Any {
        return value.toString()
    }

    private fun getUUID(value: Any): Any {
        return {
            val pgo = PGobject()
            pgo.type = "uuid"
            pgo.value = value.toString()
            pgo
        }
    }
}
