package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoType
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.format.DateTimeFormatter

class DateType : TestacoType<String> {
  override fun read(columnName: String, rs: ResultSet): String? {
    val i: Date = rs.getDate(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      DateTimeFormatter.ISO_DATE.format(i.toLocalDate())
    }
  }

  override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.TINYINT)
    } else {
      statement.setDate(columnIndex, Date.valueOf(value))
    }
  }
}