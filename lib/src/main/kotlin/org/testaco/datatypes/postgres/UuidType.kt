package org.testaco.datatypes.postgres

import org.postgresql.util.PGobject
import java.sql.PreparedStatement
import java.sql.Types

class UuidType : StringType() {
  override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.OTHER)
    } else {
      statement.setObject(columnIndex, {
        val pgo = PGobject()
        pgo.type = "uuid"
        pgo.value = value
        pgo
      })
    }
  }
}
