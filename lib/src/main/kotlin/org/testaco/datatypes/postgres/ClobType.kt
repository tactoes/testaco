package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoType
import java.io.StringReader
import java.sql.Clob
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

open class ClobType : TestacoType<String?> {
  override fun read(columnName: String, rs: ResultSet): String? {
    val i: Clob = rs.getClob(columnName)
    return try {
      if (rs.wasNull()) {
        null
      } else {
        i.characterStream.readText()
      }
    } finally {
      i.free()
    }
  }

  override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.CLOB)
    } else {
      statement.setClob(columnIndex, StringReader(value))
    }
  }
}