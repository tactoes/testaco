package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoType
import java.sql.*
import java.time.format.DateTimeFormatter

class TimeType : TestacoType<String> {
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