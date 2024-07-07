package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.format.DateTimeFormatter

data class TimestampType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<String>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): JsonNode {
    val i: Timestamp = rs.getTimestamp(columnName)
    return if (rs.wasNull()) {
      NullNode.instance
    } else {
      TextNode.valueOf(DateTimeFormatter.ISO_DATE_TIME.format(i.toLocalDateTime()))
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.TIMESTAMP)
    } else {
      when (value) {
        is TextNode -> statement.setTimestamp(columnIndex, Timestamp.valueOf(value.textValue()))
        else -> throw IllegalStateException("Timestamp database types require json data sets to store values as TextNodes")
      }
    }
  }
}