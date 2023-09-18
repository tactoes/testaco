package org.testaco.datatypes

import org.testaco.datatypes.TestacoType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

data class DoubleType(override val name: String) : TestacoType<Double>(name) {
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
      statement.setNull(columnIndex, Types.DOUBLE)
    } else {
      statement.setDouble(columnIndex, value)
    }
  }
}