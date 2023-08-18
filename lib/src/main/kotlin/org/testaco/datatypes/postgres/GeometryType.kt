package org.testaco.datatypes.postgres

import org.postgis.PGgeometry
import org.testaco.datatypes.TestacoStringType
import java.sql.*

class GeometryType : TestacoStringType() {
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
