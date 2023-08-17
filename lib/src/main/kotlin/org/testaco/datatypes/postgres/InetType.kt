package org.testaco.datatypes.postgres

import org.postgresql.util.PGobject
import org.testaco.datatypes.TestacoStringType
import java.sql.*

class InetType : TestacoStringType() {
    override fun store(value: String, columnIndex: Int, statement: PreparedStatement) {
        statement.setObject(columnIndex, {
            val pgo = PGobject()
            pgo.type = "inet"
            pgo.value = value.toString()
            pgo
        })
    }
}
