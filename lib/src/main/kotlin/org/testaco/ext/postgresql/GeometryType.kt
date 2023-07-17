package org.testaco.ext.postgresql

import org.postgis.PGgeometry
import org.testaco.dataset.datatype.AbstractDataType
import java.sql.*

class GeometryType : AbstractDataType<String>("geometry", Types.OTHER, String::class, false, false) {
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
    override fun setSqlValue(value: Any, column: Int, statement: PreparedStatement) {
        statement.setObject(
            column,
            getGeometry(value)
        )
    }

    override fun typeCast(value: Any): Any {
        return value.toString()
    }

    private fun getGeometry(value: Any): PGgeometry {
        return PGgeometry(value.toString())
    }
}
