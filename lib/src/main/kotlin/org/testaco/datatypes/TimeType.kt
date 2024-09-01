package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Time
import java.sql.Types
import java.time.format.DateTimeFormatter

data class TimeType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<String>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): JsonNode {
    val i: Time = rs.getTime(columnName)
    return if (rs.wasNull()) {
      NullNode.instance
    } else {
      TextNode.valueOf(DateTimeFormatter.ISO_TIME.format(i.toLocalTime()))
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.TIME)
    } else {
      when (value) {
        is TextNode -> statement.setTime(columnIndex, Time.valueOf(value.textValue()))
        else -> error("Time database types require json data sets to store values as TextNodes")
      }
    }
  }

}