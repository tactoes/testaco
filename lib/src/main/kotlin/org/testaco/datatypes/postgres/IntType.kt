package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class IntType : TestacoType<Int?> {
  override fun read(columnName: String, rs: ResultSet): Int? {
    val i = rs.getInt(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      i
    }
  }

  override fun store(value: Int?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.TINYINT)
    } else {
      statement.setInt(columnIndex, value)
    }
  }
}