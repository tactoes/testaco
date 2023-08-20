package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.Base64

open class BinaryType : TestacoType<String> {
  override fun read(columnName: String, rs: ResultSet): String? {
    val i = rs.getBytes(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      Base64.getEncoder().encodeToString(i)
    }
  }

  override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.TINYINT)
    } else {
      statement.setBytes(columnIndex, Base64.getDecoder().decode(value))
    }
  }
}