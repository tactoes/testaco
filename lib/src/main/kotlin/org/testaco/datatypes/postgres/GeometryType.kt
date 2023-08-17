package org.testaco.datatypes.postgres

import org.postgis.PGgeometry
import org.testaco.datatypes.TestacoStringType
import java.sql.*

class GeometryType : TestacoStringType() {
    override fun store(value: String, columnIndex: Int, statement: PreparedStatement) {
        statement.setObject(
            columnIndex,
            PGgeometry(value.toString())
        )
    }
}
