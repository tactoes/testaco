package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ShortNode
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

data class IntType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<Int?>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): JsonNode {
    val i = rs.getInt(columnName)
    return if (rs.wasNull()) {
      NullNode.instance
    } else {
      IntNode.valueOf(i)
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.INTEGER)
    } else {
      when (value) {
        is IntNode -> statement.setInt(columnIndex, value.intValue())
        is ShortNode -> statement.setInt(columnIndex, value.shortValue().toInt())
        else -> error("Integer database types require json data sets to store values as Short or IntNodes")
      }
    }
  }
}