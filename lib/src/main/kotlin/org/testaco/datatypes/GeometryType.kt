package org.testaco.datatypes

import org.postgis.PGgeometry
import java.sql.*

data class GeometryType(override val name: String) : TestacoType<String>(name) {
    override fun read(columnName: String, rs: ResultSet): String? {
        val i = rs.getString(columnName)
        return if (rs.wasNull()) {
            null
        } else {
            i
        }
    }
    override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
        if (value == null) {
            statement.setNull(columnIndex, Types.OTHER)
        } else {
            statement.setObject(
                columnIndex,
                PGgeometry(value.toString())
            )
        }
    }
}
