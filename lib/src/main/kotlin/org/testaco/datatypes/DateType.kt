package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.format.DateTimeFormatter

data class DateType(override val name: String, override val sqlType: Int, override val sqlTypeName: String,
                    override val allowedTimeDiffInSeconds: Int)
  : DateTimeHandling<String>(name, sqlType, sqlTypeName, allowedTimeDiffInSeconds) {
  override fun read(columnName: String, rs: ResultSet): JsonNode {
    val i: Date = rs.getDate(columnName)
    return if (rs.wasNull()) {
      NullNode.instance
    } else {
      TextNode.valueOf(DateTimeFormatter.ISO_DATE.format(i.toLocalDate()))
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.DATE)
    } else {
      when (value) {
        is TextNode -> statement.setDate(columnIndex, Date.valueOf(value.textValue()))
        else -> error("Date database types require json data sets to store values as TextNodes")
      }
    }
  }

}