package org.testaco.datatypes.postgres

import org.postgresql.util.PGobject
import org.testaco.datatypes.TestacoConverter
import java.sql.PreparedStatement
import java.sql.ResultSet

class UuidType : TestacoConverter<String> {

  override fun read(columnName: String, rs: ResultSet): String? {
    val s = rs.getString(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      s
    }
  }

  override fun store(value: String, columnIndex: Int, statement: PreparedStatement) {
    statement.setObject(columnIndex, {
      val pgo = PGobject()
      pgo.type = "uuid"
      pgo.value = value
      pgo
    })
  }
}
