package org.testaco.datatypes.postgres

import org.postgis.PGgeometry
import org.testaco.datatypes.TestacoStringConverter
import java.sql.*

class GeometryType : TestacoStringConverter() {
    override fun store(value: String, columnIndex: Int, statement: PreparedStatement) {
        statement.setObject(
            columnIndex,
            PGgeometry(value.toString())
        )
    }
}
