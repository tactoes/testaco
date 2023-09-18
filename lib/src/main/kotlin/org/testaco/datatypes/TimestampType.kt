package org.testaco.datatypes

import org.testaco.datatypes.TestacoType
import java.sql.*
import java.time.format.DateTimeFormatter

data class TimestampType(override val name: String) : TestacoType<String>(name) {
  override fun read(columnName: String, rs: ResultSet): String? {
    val i: Timestamp = rs.getTimestamp(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      DateTimeFormatter.ISO_DATE_TIME.format(i.toLocalDateTime())
    }
  }

  override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.TIMESTAMP)
    } else {
      statement.setTimestamp(columnIndex, Timestamp.valueOf(value))
    }
  }
}