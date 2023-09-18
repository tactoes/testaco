package org.testaco.datatypes

import org.testaco.datatypes.TestacoType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

data class StringType(override val name: String) : TestacoType<String>(name) {
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
      statement.setNull(columnIndex, Types.VARCHAR)
    } else {
      statement.setString(columnIndex, value)
    }
  }
}