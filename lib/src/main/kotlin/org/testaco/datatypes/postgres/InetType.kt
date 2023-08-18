package org.testaco.datatypes.postgres

import org.postgresql.util.PGobject
import org.testaco.datatypes.TestacoStringType
import java.sql.*

class InetType : TestacoStringType() {
    override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
        if (value == null) {
            statement.setNull(columnIndex, Types.OTHER)
        } else {
            statement.setObject(columnIndex, {
                val pgo = PGobject()
                pgo.type = "inet"
                pgo.value = value.toString()
                pgo
            })
        }
    }
}
