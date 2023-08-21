package org.testaco.datatypes.postgres

import org.testaco.datatypes.TestacoType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class LongType : TestacoType<Long> {
  override fun read(columnName: String, rs: ResultSet): Long? {
    val i = rs.getLong(columnName)
    return if (rs.wasNull()) {
      null
    } else {
      i
    }
  }

  override fun store(value: Long?, columnIndex: Int, statement: PreparedStatement) {
    if (value == null) {
      statement.setNull(columnIndex, Types.BIGINT)
    } else {
      statement.setLong(columnIndex, value)
    }
  }
}