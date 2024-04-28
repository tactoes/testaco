package org.testaco.datatypes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.BinaryNode
import com.fasterxml.jackson.databind.node.TextNode
import java.io.ByteArrayInputStream
import java.sql.Blob
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.Base64

data class BlobType(override val name: String, override val sqlType: Int, override val sqlTypeName: String) : TestacoType<String>(name, sqlType, sqlTypeName) {
  override fun read(columnName: String, rs: ResultSet): String? {
    val i: Blob = rs.getBlob(columnName)
    return try {
      if (rs.wasNull()) {
        null
      } else {
        Base64.getEncoder().encodeToString(i.binaryStream.readAllBytes())
      }
    } finally {
      i.free()
    }
  }

  override fun store(value: JsonNode?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.BLOB)
    } else {
      when (value) {
        /*
        You are not meant to use testaco for non-trivial amounts of data. This is likely to ruin your day if you do.
         */
        is TextNode -> statement.setBlob(columnIndex, ByteArrayInputStream(Base64.getDecoder().decode(value.textValue())))
        else -> throw IllegalStateException("Blob database types require json data sets to store values as TextNodes")
      }
    }
  }
}