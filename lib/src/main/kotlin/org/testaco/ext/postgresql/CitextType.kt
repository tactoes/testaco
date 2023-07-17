package org.testaco.ext.postgresql

import org.postgresql.util.PGobject
import org.testaco.dataset.datatype.AbstractDataType
import java.sql.*

class CitextType : AbstractDataType<String>("citext", Types.OTHER, String::class, false, false) {
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
        statement.setObject(column, getCitext(value))
    }

    override fun typeCast(value: Any): String {
        return value.toString()
    }

    private fun getCitext(value: Any): PGobject {
        val pgo = PGobject()
        pgo.type = "citext"
        pgo.value = value.toString()
        return pgo
    }
}
