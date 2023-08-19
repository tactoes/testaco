package org.testaco.datatypes.postgres

import org.postgresql.util.PGInterval
import java.sql.*

class IntervalType : StringType() {
    override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
        if (value == null) {
            statement.setNull(columnIndex, Types.OTHER)
        } else {
            statement.setObject(columnIndex, PGInterval(value))
        }
    }
}