package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

data class DoubleType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<Double>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): JsonNode {
    val i = rs.getDouble(columnName)
    return if (rs.wasNull()) {
      NullNode.instance
    } else {
      DoubleNode.valueOf(i)
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.DOUBLE)
    } else {
      when (value) {
        is DoubleNode -> statement.setDouble(columnIndex, value.doubleValue())
        is FloatNode -> statement.setDouble(columnIndex, value.floatValue().toDouble())
        is TextNode -> statement.setDouble(columnIndex, value.textValue().toDouble())
        else -> error("Double database types require json data sets to store values as Float, Double, or TextNodes")
      }
    }
  }

}