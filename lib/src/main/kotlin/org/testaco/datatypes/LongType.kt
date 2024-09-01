package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ShortNode
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

data class LongType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<Long>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): JsonNode {
    val i = rs.getLong(columnName)
    return if (rs.wasNull()) {
      NullNode.instance
    } else {
      LongNode.valueOf(i)
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.BIGINT)
    } else {
      when (value) {
        is LongNode -> statement.setLong(columnIndex, value.longValue())
        is IntNode -> statement.setLong(columnIndex, value.intValue().toLong())
        is ShortNode -> statement.setLong(columnIndex, value.shortValue().toLong())
        else -> error("Long database types require json data sets to store values as Short, Int or LongNodes")
      }
    }
  }

}