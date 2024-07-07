package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

data class StringType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<String>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): JsonNode {
    val i = rs.getString(columnName)
    return if (rs.wasNull()) {
      NullNode.instance
    } else {
      TextNode.valueOf(i)
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.VARCHAR)
    } else {
      when (value) {
        is TextNode -> statement.setString(columnIndex, value.textValue())
        else -> throw IllegalStateException("String database types require json data sets to store values as TextNodes")
      }
    }
  }
}