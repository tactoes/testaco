package org.testaco.datatypes

import org.testaco.datatypes.TestacoType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

data class BooleanType(override val name: String) : TestacoType<Boolean?>(name) {
  override fun read(columnName: String, rs: ResultSet): Boolean? {
    val i = rs.getBoolean(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      i
    }
  }

  override fun store(value: Boolean?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.BOOLEAN)
    } else {
      statement.setBoolean(columnIndex, value)
    }
  }
}