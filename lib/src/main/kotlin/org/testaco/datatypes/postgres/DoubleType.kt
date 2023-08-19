package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class DoubleType : TestacoType<Double> {
  override fun read(columnName: String, rs: ResultSet): Double? {
    val i = rs.getDouble(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      i
    }
  }

  override fun store(value: Double?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.TINYINT)
    } else {
      statement.setDouble(columnIndex, value)
    }
  }
}