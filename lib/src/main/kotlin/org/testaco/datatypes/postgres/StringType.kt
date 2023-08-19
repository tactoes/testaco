package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

open class StringType : TestacoType<String> {
  override fun read(columnName: String, rs: ResultSet): String? {
    val i = rs.getString(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      i
    }
  }

  override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.TINYINT)
    } else {
      statement.setString(columnIndex, value)
    }
  }
}