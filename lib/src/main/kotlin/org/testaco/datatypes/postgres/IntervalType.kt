package org.testaco.datatypes.postgres

import org.postgresql.util.PGInterval
import org.testaco.datatypes.TestacoStringConverter
import java.sql.*

class IntervalType : TestacoStringConverter() {
    override fun store(value: String, columnIndex: Int, statement: PreparedStatement) {
        statement.setObject(columnIndex, PGInterval(value))
    }
}