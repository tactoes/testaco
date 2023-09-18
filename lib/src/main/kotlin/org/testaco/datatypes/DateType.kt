package org.testaco.datatypes

import org.testaco.datatypes.TestacoType
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.format.DateTimeFormatter

data class DateType(override val name: String) : TestacoType<String>(name) {
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
      statement.setNull(columnIndex, Types.DATE)
    } else {
      statement.setDate(columnIndex, Date.valueOf(value))
    }
  }
}