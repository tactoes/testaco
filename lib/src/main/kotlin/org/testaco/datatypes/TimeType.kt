package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import org.testaco.datatypes.TestacoType
import java.sql.*
import java.time.format.DateTimeFormatter

data class TimeType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<String>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): String? {
    val i: Time = rs.getTime(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      DateTimeFormatter.ISO_TIME.format(i.toLocalTime())
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.TIME)
    } else {
      when (value) {
        is TextNode -> statement.setTime(columnIndex, Time.valueOf(value.textValue()))
        else -> throw IllegalStateException("Time database types require json data sets to store values as TextNodes")
      }
    }
  }

}