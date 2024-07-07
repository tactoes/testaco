package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import org.testaco.datatypes.TestacoType
import java.io.ByteArrayInputStream
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.Base64

data class BinaryType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<String>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): JsonNode {
    val i = rs.getBytes(columnName)
    return if (rs.wasNull()) {
      NullNode.instance
    } else {
      TextNode.valueOf(Base64.getEncoder().encodeToString(i))
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, sqlType)
    } else {
      when (value) {
        /*
        You are not meant to use testaco for non-trivial amounts of data. This is likely to ruin your day if you do.
         */
        is BinaryNode -> statement.setBinaryStream(columnIndex, ByteArrayInputStream(value.binaryValue()))
        is TextNode -> statement.setBinaryStream(columnIndex, ByteArrayInputStream(Base64.getDecoder().decode(value.textValue())))
        else -> throw IllegalStateException("Big decimal database types require json data sets to store values as Binary or TextNodes")
      }
    }
  }

}