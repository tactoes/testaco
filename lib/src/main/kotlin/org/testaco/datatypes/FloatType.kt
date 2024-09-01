package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.FloatNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

data class FloatType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<Float>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): JsonNode {
    val i = rs.getFloat(columnName)
    return if (rs.wasNull()) {
      NullNode.instance
    } else {
      FloatNode.valueOf(i)
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.FLOAT)
    } else {
      when (value) {
        is FloatNode -> statement.setFloat(columnIndex, value.floatValue())
        is TextNode -> statement.setFloat(columnIndex, value.textValue().toFloat())
        else -> error("Float database types require json data sets to store values as Float or TextNodes")
      }
    }
  }

}