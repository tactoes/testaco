package org.testaco.datatypes.postgres

import org.postgresql.util.PGInterval
import org.testaco.datatypes.TestacoStringType
import java.sql.*

class IntervalType : TestacoStringType() {
    override fun store(value: String, columnIndex: Int, statement: PreparedStatement) {
        statement.setObject(columnIndex, PGInterval(value))
    }
}