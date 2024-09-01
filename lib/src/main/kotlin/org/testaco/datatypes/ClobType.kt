package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import java.io.StringReader
import java.sql.Clob
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

data class ClobType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<String?>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): JsonNode {
    val i: Clob = rs.getClob(columnName)
    return try {
      if (rs.wasNull()) {
        NullNode.instance
      } else {
        TextNode.valueOf(i.characterStream.readText())
      }
    } finally {
      i.free()
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.CLOB)
    } else {
      when (value) {
        /*
        You are not meant to use testaco for non-trivial amounts of data. This is likely to ruin your day if you do.
         */
        is TextNode -> statement.setClob(columnIndex, StringReader(value.textValue()))
        else -> error("Clob database types require json data sets to store values as TextNodes")
      }
    }
  }

}