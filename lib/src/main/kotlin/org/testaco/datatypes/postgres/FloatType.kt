package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class FloatType : TestacoType<Float> {
  override fun read(columnName: String, rs: ResultSet): Float? {
    val i = rs.getFloat(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      i
    }
  }

  override fun store(value: Float?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.FLOAT)
    } else {
      statement.setFloat(columnIndex, value)
    }
  }
}