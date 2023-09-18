package org.testaco.datatypes

import org.testaco.datatypes.TestacoType
import java.sql.*
import java.time.format.DateTimeFormatter

data class TimeType(override val name: String) : TestacoType<String>(name) {
  override fun read(columnName: String, rs: ResultSet): String? {
    val i: Time = rs.getTime(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      DateTimeFormatter.ISO_TIME.format(i.toLocalTime())
    }
  }

  override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.TIME)
    } else {
      statement.setTime(columnIndex, Time.valueOf(value))
    }
  }
}