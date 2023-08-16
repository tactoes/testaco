package org.testaco.datatypes.postgres

import org.postgresql.util.PGobject
import org.testaco.datatypes.TestacoStringConverter
import java.sql.PreparedStatement

class UuidType : TestacoStringConverter() {
  override fun store(value: String, columnIndex: Int, statement: PreparedStatement) {
    statement.setObject(columnIndex, {
      val pgo = PGobject()
      pgo.type = "uuid"
      pgo.value = value
      pgo
    })
  }
}
