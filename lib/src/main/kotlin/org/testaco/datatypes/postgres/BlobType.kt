package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoType
import java.sql.Blob
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.Base64

open class BlobType : TestacoType<String> {
  override fun read(columnName: String, rs: ResultSet): String? {
    val i: Blob = rs.getBlob(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      Base64.getEncoder().encodeToString(i.binaryStream.readAllBytes())
    }
  }

  override fun store(value: String?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.BLOB)
    } else {
      statement.setBlob(columnIndex, Base64.getDecoder().decode(value).inputStream())
    }
  }
}